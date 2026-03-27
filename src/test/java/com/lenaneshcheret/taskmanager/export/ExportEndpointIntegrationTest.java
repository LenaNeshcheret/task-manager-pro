package com.lenaneshcheret.taskmanager.export;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.hamcrest.Matchers.containsString;

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
class ExportEndpointIntegrationTest {

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
    registry.add("app.exports.storage-dir", () -> "target/test-exports");
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
  void requestExportAwaitDoneAndDownloadCsv() throws InterruptedException {
    String token = obtainAccessToken("user1", "user1pass");
    Number projectId = createProject(token, uniqueName("project"));

    createTask(token, projectId.longValue(), "alpha export task", Instant.parse("2034-01-01T10:00:00Z"));
    createTask(token, projectId.longValue(), "beta export task", Instant.parse("2034-01-02T10:00:00Z"));

    Number jobId = given()
        .auth()
        .oauth2(token)
        .contentType(ContentType.JSON)
        .body(Map.of(
            "projectId", projectId.longValue(),
            "type", "CSV"
        ))
        .when()
        .post("/api/v1/exports")
        .then()
        .statusCode(202)
        .extract()
        .path("jobId");

    awaitDone(token, jobId.longValue());

    String csv = given()
        .auth()
        .oauth2(token)
        .when()
        .get("/api/v1/exports/{jobId}/download", jobId.longValue())
        .then()
        .statusCode(200)
        .header("Content-Type", containsString("text/csv"))
        .extract()
        .asString();

    assertTrue(csv.contains("id,title,description,status,dueAt,completedAt,priority"));
    assertTrue(csv.contains("alpha export task"));
    assertTrue(csv.contains("beta export task"));
  }

  @Test
  void nonOwnerGets404ForStatusAndDownload() throws InterruptedException {
    String ownerToken = obtainAccessToken("user1", "user1pass");
    String otherToken = obtainAccessToken("user2", "user2pass");

    Number projectId = createProject(ownerToken, uniqueName("project"));
    createTask(ownerToken, projectId.longValue(), "private export task", Instant.parse("2034-02-01T10:00:00Z"));

    Number jobId = given()
        .auth()
        .oauth2(ownerToken)
        .contentType(ContentType.JSON)
        .body(Map.of(
            "projectId", projectId.longValue(),
            "type", "CSV"
        ))
        .when()
        .post("/api/v1/exports")
        .then()
        .statusCode(202)
        .extract()
        .path("jobId");

    awaitDone(ownerToken, jobId.longValue());

    given()
        .auth()
        .oauth2(otherToken)
        .when()
        .get("/api/v1/exports/{jobId}", jobId.longValue())
        .then()
        .statusCode(404);

    given()
        .auth()
        .oauth2(otherToken)
        .when()
        .get("/api/v1/exports/{jobId}/download", jobId.longValue())
        .then()
        .statusCode(404);
  }

  private void awaitDone(String token, Long jobId) throws InterruptedException {
    Instant deadline = Instant.now().plusSeconds(10);

    while (Instant.now().isBefore(deadline)) {
      Map<String, Object> response = given()
          .auth()
          .oauth2(token)
          .when()
          .get("/api/v1/exports/{jobId}", jobId)
          .then()
          .statusCode(200)
          .extract()
          .as(Map.class);

      String status = (String) response.get("status");
      if ("DONE".equals(status)) {
        return;
      }
      if ("FAILED".equals(status)) {
        fail("Export job failed: " + response.get("errorMessage"));
      }

      Thread.sleep(200);
    }

    fail("Export job did not finish within timeout");
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
