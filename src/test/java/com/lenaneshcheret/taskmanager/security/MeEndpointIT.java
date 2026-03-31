package com.lenaneshcheret.taskmanager.security;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.nio.file.Path;
import java.time.Duration;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class MeEndpointIT {

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
  void meEndpointReturns401WithoutToken() {
    given()
        .when()
        .get("/api/v1/me")
        .then()
        .statusCode(401);
  }

  @Test
  void meEndpointReturnsEmailAndRolesWithValidToken() {
    String accessToken = obtainAccessToken("user2", "user2pass");

    given()
        .auth()
        .oauth2(accessToken)
        .when()
        .get("/api/v1/me")
        .then()
        .statusCode(200)
        .body("email", equalTo("user2@example.com"))
        .body("roles", hasItems("ROLE_USER", "ROLE_ADMIN"));
  }

  @Test
  void nonVersionedMeRouteReturns404WithValidToken() {
    String accessToken = obtainAccessToken("user2", "user2pass");

    given()
        .auth()
        .oauth2(accessToken)
        .when()
        .get("/api/me")
        .then()
        .statusCode(404);
  }

  @Test
  void healthEndpointWorksOnVersionedAndLegacyPaths() {
    given()
        .when()
        .get("/api/v1/health")
        .then()
        .statusCode(200)
        .body(equalTo("task-manager-pro is running"));

    given()
        .when()
        .get("/api/health")
        .then()
        .statusCode(200)
        .body(equalTo("task-manager-pro is running"));
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

  private static String keycloakBaseUrl() {
    return "http://" + KEYCLOAK.getHost() + ":" + KEYCLOAK.getMappedPort(8080);
  }
}
