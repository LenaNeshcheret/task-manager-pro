package com.lenaneshcheret.taskmanager.project;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.nio.file.Path;
import java.time.Duration;
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
class ProjectEndpointIT {

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
  void userACanCreateListGetUpdateAndDeleteOwnProject() {
    String userAToken = obtainAccessToken("user1", "user1pass");
    String initialName = uniqueName("alpha");
    String updatedName = uniqueName("alpha-updated");

    Number projectId = given()
        .auth()
        .oauth2(userAToken)
        .contentType(ContentType.JSON)
        .body(Map.of("name", initialName))
        .when()
        .post("/api/v1/projects")
        .then()
        .statusCode(201)
        .body("name", equalTo(initialName))
        .extract()
        .path("id");

    given()
        .auth()
        .oauth2(userAToken)
        .when()
        .get("/api/v1/projects")
        .then()
        .statusCode(200)
        .body("name", hasItem(initialName));

    given()
        .auth()
        .oauth2(userAToken)
        .when()
        .get("/api/v1/projects/{id}", projectId.longValue())
        .then()
        .statusCode(200)
        .body("id", equalTo(projectId.intValue()))
        .body("name", equalTo(initialName));

    given()
        .auth()
        .oauth2(userAToken)
        .contentType(ContentType.JSON)
        .body(Map.of("name", updatedName))
        .when()
        .patch("/api/v1/projects/{id}", projectId.longValue())
        .then()
        .statusCode(200)
        .body("id", equalTo(projectId.intValue()))
        .body("name", equalTo(updatedName));

    given()
        .auth()
        .oauth2(userAToken)
        .when()
        .delete("/api/v1/projects/{id}", projectId.longValue())
        .then()
        .statusCode(204);

    given()
        .auth()
        .oauth2(userAToken)
        .when()
        .get("/api/v1/projects/{id}", projectId.longValue())
        .then()
        .statusCode(404);
  }

  @Test
  void userBCannotAccessUserAProject() {
    String userAToken = obtainAccessToken("user1", "user1pass");
    String userBToken = obtainAccessToken("user2", "user2pass");
    Number projectId = createProject(userAToken, uniqueName("shared"));

    given()
        .auth()
        .oauth2(userBToken)
        .when()
        .get("/api/v1/projects/{id}", projectId.longValue())
        .then()
        .statusCode(404);

    given()
        .auth()
        .oauth2(userBToken)
        .contentType(ContentType.JSON)
        .body(Map.of("name", uniqueName("attempt-update")))
        .when()
        .patch("/api/v1/projects/{id}", projectId.longValue())
        .then()
        .statusCode(404);

    given()
        .auth()
        .oauth2(userBToken)
        .when()
        .delete("/api/v1/projects/{id}", projectId.longValue())
        .then()
        .statusCode(404);
  }

  @Test
  void createProjectRejectsBlankName() {
    String userAToken = obtainAccessToken("user1", "user1pass");

    given()
        .auth()
        .oauth2(userAToken)
        .contentType(ContentType.JSON)
        .body(Map.of("name", "   "))
        .when()
        .post("/api/v1/projects")
        .then()
        .statusCode(400);
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
    return "project-" + prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
  }

  private static String keycloakBaseUrl() {
    return "http://" + KEYCLOAK.getHost() + ":" + KEYCLOAK.getMappedPort(8080);
  }
}
