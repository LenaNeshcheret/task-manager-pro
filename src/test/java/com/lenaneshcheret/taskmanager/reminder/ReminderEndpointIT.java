package com.lenaneshcheret.taskmanager.reminder;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lenaneshcheret.taskmanager.testsupport.ReminderPipelineTestHelper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(ReminderPipelineTestHelper.class)
class ReminderEndpointIT {

  private static final Path REALM_IMPORT_FILE = Path.of("infra", "keycloak", "realm-export.json")
      .toAbsolutePath();

  private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
      DockerImageName.parse("postgres:16-alpine")
  );

  private static final GenericContainer<?> KEYCLOAK = new GenericContainer<>(
      DockerImageName.parse("quay.io/keycloak/keycloak:24.0")
  )
      .withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", "admin")
      .withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", "admin")
      .withCommand("start-dev", "--import-realm")
      .withCopyFileToContainer(
          MountableFile.forHostPath(REALM_IMPORT_FILE.toString()),
          "/opt/keycloak/data/import/realm-export.json"
      )
      .withExposedPorts(8080)
      .waitingFor(Wait.forHttp("/realms/task-manager/.well-known/openid-configuration").forStatusCode(200))
      .withStartupTimeout(Duration.ofMinutes(3));

  static {
    Startables.deepStart(Stream.of(POSTGRES, KEYCLOAK)).join();
  }

  @Autowired
  private ReminderPipelineTestHelper reminderPipelineTestHelper;

  @LocalServerPort
  private int port;

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add(
        "spring.security.oauth2.resourceserver.jwt.issuer-uri",
        () -> keycloakBaseUrl() + "/realms/task-manager"
    );
    registry.add("app.security.oauth2.client-id", () -> "task-manager-api");
    registry.add("app.reminders.due-window", () -> "PT30M");
    registry.add("app.reminders.enqueue-fixed-delay-ms", () -> "3600000");
    registry.add("app.reminders.dispatch-fixed-delay-ms", () -> "100");
    registry.add("app.reminders.dispatch-batch-size", () -> "10");
  }

  @BeforeEach
  void setupRestAssured() {
    RestAssured.baseURI = "http://localhost";
    RestAssured.port = port;
  }

  @AfterAll
  static void stopContainers() {
    KEYCLOAK.stop();
    POSTGRES.stop();
  }

  @Test
  void enqueueIsIdempotentAndAsyncWorkerSendsReminder() throws InterruptedException {
    String token = obtainAccessToken("user1", "user1pass");
    Number projectId = createProject(token, uniqueName("project"));
    Number taskId = createTask(
        token,
        projectId.longValue(),
        "due soon task",
        Instant.now().plus(Duration.ofMinutes(5))
    );

    int created = reminderPipelineTestHelper.enqueueDueSoonReminders();
    assertEquals(1, created);

    List<Map<String, Object>> reminders = listReminders(token, null);
    assertEquals(1, reminders.size());
    assertEquals(taskId.intValue(), ((Number) reminders.get(0).get("taskId")).intValue());

    awaitUntilSent(token);

    int createdAgain = reminderPipelineTestHelper.enqueueDueSoonReminders();
    assertEquals(0, createdAgain);

    List<Map<String, Object>> remindersAfterSecondRun = listReminders(token, null);
    assertEquals(1, remindersAfterSecondRun.size());
    assertEquals("SENT", remindersAfterSecondRun.get(0).get("status"));
  }

  private void awaitUntilSent(String token) throws InterruptedException {
    Instant deadline = Instant.now().plusSeconds(10);

    while (Instant.now().isBefore(deadline)) {
      reminderPipelineTestHelper.dispatchPendingReminders();

      List<Map<String, Object>> sentReminders = listReminders(token, "SENT");
      if (sentReminders.size() == 1) {
        return;
      }
      Thread.sleep(200);
    }

    List<Map<String, Object>> allReminders = listReminders(token, null);
    assertTrue(
        allReminders.stream().anyMatch(reminder -> "SENT".equals(reminder.get("status"))),
        "Expected reminder to transition to SENT within timeout"
    );
  }

  private List<Map<String, Object>> listReminders(String token, String status) {
    var request = given()
        .auth()
        .oauth2(token);

    if (status != null) {
      request.queryParam("status", status);
    }

    return request
        .when()
        .get("/api/v1/reminders")
        .then()
        .statusCode(200)
        .extract()
        .jsonPath()
        .getList("$");
  }

  private Number createProject(String token, String name) {
    return given()
        .auth()
        .oauth2(token)
        .contentType(ContentType.JSON)
        .body(Map.of("name", name))
        .when()
        .post("/api/v1/projects")
        .then()
        .statusCode(201)
        .extract()
        .path("id");
  }

  private Number createTask(String token, Long projectId, String title, Instant dueAt) {
    return given()
        .auth()
        .oauth2(token)
        .contentType(ContentType.JSON)
        .body(Map.of(
            "title", title,
            "description", "desc-" + title,
            "dueAt", dueAt.toString(),
            "priority", "MEDIUM"
        ))
        .when()
        .post("/api/v1/projects/{projectId}/tasks", projectId)
        .then()
        .statusCode(201)
        .extract()
        .path("id");
  }

  private static String obtainAccessToken(String username, String password) {
    return given()
        .baseUri(keycloakBaseUrl())
        .contentType(ContentType.URLENC)
        .formParam("grant_type", "password")
        .formParam("client_id", "task-manager-api")
        .formParam("username", username)
        .formParam("password", password)
        .when()
        .post("/realms/task-manager/protocol/openid-connect/token")
        .then()
        .statusCode(200)
        .extract()
        .path("access_token");
  }

  private static String uniqueName(String prefix) {
    return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
  }

  private static String keycloakBaseUrl() {
    return "http://" + KEYCLOAK.getHost() + ":" + KEYCLOAK.getMappedPort(8080);
  }
}
