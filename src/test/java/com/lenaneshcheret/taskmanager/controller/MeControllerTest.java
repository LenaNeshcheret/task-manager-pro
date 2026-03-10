package com.lenaneshcheret.taskmanager.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class MeControllerTest {

  private final MeController controller = new MeController();

  @Test
  void meReturnsEmailClaimAndRoleAuthoritiesOnly() {
    Jwt jwt = Jwt.withTokenValue("token")
        .header("alg", "none")
        .subject("subject-user")
        .claim("email", "user@example.com")
        .build();

    JwtAuthenticationToken authentication = new JwtAuthenticationToken(
        jwt,
        List.of(
            new SimpleGrantedAuthority("ROLE_ADMIN"),
            new SimpleGrantedAuthority("SCOPE_profile"),
            new SimpleGrantedAuthority("ROLE_USER")
        ),
        "principal-name"
    );

    MeController.MeResponse response = controller.me(authentication);

    assertThat(response.email()).isEqualTo("user@example.com");
    assertThat(response.roles()).containsExactly("ROLE_ADMIN", "ROLE_USER");
  }

  @Test
  void meUsesPreferredUsernameWhenEmailMissing() {
    Jwt jwt = Jwt.withTokenValue("token")
        .header("alg", "none")
        .subject("subject-user")
        .claim("preferred_username", "preferred-user")
        .build();

    JwtAuthenticationToken authentication = new JwtAuthenticationToken(
        jwt,
        List.of(new SimpleGrantedAuthority("SCOPE_profile")),
        "principal-name"
    );

    MeController.MeResponse response = controller.me(authentication);

    assertThat(response.email()).isEqualTo("preferred-user");
    assertThat(response.roles()).isEmpty();
  }

  @Test
  void meUsesPreferredUsernameWhenEmailBlank() {
    Jwt jwt = Jwt.withTokenValue("token")
        .header("alg", "none")
        .subject("subject-user")
        .claim("email", "  ")
        .claim("preferred_username", "preferred-user")
        .build();

    JwtAuthenticationToken authentication = new JwtAuthenticationToken(
        jwt,
        List.of(new SimpleGrantedAuthority("ROLE_USER")),
        "principal-name"
    );

    MeController.MeResponse response = controller.me(authentication);

    assertThat(response.email()).isEqualTo("preferred-user");
    assertThat(response.roles()).containsExactly("ROLE_USER");
  }

  @Test
  void meFallsBackToPrincipalNameWhenEmailAndUsernameMissing() {
    Jwt jwt = Jwt.withTokenValue("token")
        .header("alg", "none")
        .subject("subject-user")
        .build();

    JwtAuthenticationToken authentication = new JwtAuthenticationToken(
        jwt,
        List.of(new SimpleGrantedAuthority("SCOPE_profile")),
        "principal-name"
    );

    MeController.MeResponse response = controller.me(authentication);

    assertThat(response.email()).isEqualTo("principal-name");
    assertThat(response.roles()).isEmpty();
  }
}
