package com.lenaneshcheret.taskmanager.task;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class TaskEndpointIT {

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
  void createTaskAndListWithFiltersAndPaging() {
    String token = obtainAccessToken("user1", "user1pass");
    Number projectId = createProject(token, uniqueName("project"));

    Instant jan10 = Instant.parse("2030-01-10T10:00:00Z");
    Instant feb10 = Instant.parse("2030-02-10T10:00:00Z");

    createTask(token, projectId.longValue(), "alpha draft", jan10);
    createTask(token, projectId.longValue(), "beta launch", feb10);
    Number doneTaskId = createTask(token, projectId.longValue(), "alpha closed", jan10.plusSeconds(7200));

    given()
        .auth()
        .oauth2(token)
        .when()
        .post("/api/v1/tasks/{taskId}/complete", doneTaskId.longValue())
        .then()
        .statusCode(200)
        .body("status", equalTo("DONE"))
        .body("completedAt", notNullValue());

    given()
        .auth()
        .oauth2(token)
        .queryParam("status", "TODO")
        .queryParam("dueFrom", "2030-01-01T00:00:00Z")
        .queryParam("dueTo", "2030-01-31T23:59:59Z")
        .queryParam("q", "alpha")
        .queryParam("page", 0)
        .queryParam("size", 1)
        .when()
        .get("/api/v1/projects/{projectId}/tasks", projectId.longValue())
        .then()
        .statusCode(200)
        .body("content.size()", equalTo(1))
        .body("content[0].title", equalTo("alpha draft"))
        .body("number", equalTo(0))
        .body("size", equalTo(1))
        .body("totalElements", equalTo(1));
  }

  @Test
  void updateWithCorrectVersionReturns200() {
    String token = obtainAccessToken("user1", "user1pass");
    Number projectId = createProject(token, uniqueName("project"));
    Number taskId = createTask(token, projectId.longValue(), "versioned task", Instant.parse("2031-01-01T08:00:00Z"));

    Number version = given()
        .auth()
        .oauth2(token)
        .when()
        .get("/api/v1/projects/{projectId}/tasks", projectId.longValue())
        .then()
        .statusCode(200)
        .extract()
        .path("content[0].version");

    given()
        .auth()
        .oauth2(token)
        .contentType(ContentType.JSON)
        .body(Map.of(
            "version", version.longValue(),
            "title", "versioned task updated",
            "priority", "HIGH"
        ))
        .when()
        .patch("/api/v1/tasks/{taskId}", taskId.longValue())
        .then()
        .statusCode(200)
        .body("id", equalTo(taskId.intValue()))
        .body("title", equalTo("versioned task updated"))
        .body("priority", equalTo("HIGH"))
        .body("version", equalTo(version.intValue() + 1));
  }

  @Test
  void updateWithStaleVersionReturns409() {
    String token = obtainAccessToken("user1", "user1pass");
    Number projectId = createProject(token, uniqueName("project"));

    Number taskId = createTask(token, projectId.longValue(), "stale task", Instant.parse("2031-03-01T08:00:00Z"));

    given()
        .auth()
        .oauth2(token)
        .contentType(ContentType.JSON)
        .body(Map.of(
            "version", 0,
            "title", "stale task first update"
        ))
        .when()
        .patch("/api/v1/tasks/{taskId}", taskId.longValue())
        .then()
        .statusCode(200);

    given()
        .auth()
        .oauth2(token)
        .contentType(ContentType.JSON)
        .body(Map.of(
            "version", 0,
            "title", "stale task second update"
        ))
        .when()
        .patch("/api/v1/tasks/{taskId}", taskId.longValue())
        .then()
        .statusCode(409);
  }

  @Test
  void userBCannotAccessUserATask() {
    String userAToken = obtainAccessToken("user1", "user1pass");
    String userBToken = obtainAccessToken("user2", "user2pass");

    Number projectId = createProject(userAToken, uniqueName("project"));
    Number taskId = createTask(userAToken, projectId.longValue(), "private task", Instant.parse("2032-01-01T08:00:00Z"));

    given()
        .auth()
        .oauth2(userBToken)
        .contentType(ContentType.JSON)
        .body(Map.of(
            "version", 0,
            "title", "hijack"
        ))
        .when()
        .patch("/api/v1/tasks/{taskId}", taskId.longValue())
        .then()
        .statusCode(404);

    given()
        .auth()
        .oauth2(userBToken)
        .when()
        .post("/api/v1/tasks/{taskId}/complete", taskId.longValue())
        .then()
        .statusCode(404);

    given()
        .auth()
        .oauth2(userBToken)
        .when()
        .delete("/api/v1/tasks/{taskId}", taskId.longValue())
        .then()
        .statusCode(404);
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
