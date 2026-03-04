package com.lenaneshcheret.taskmanager.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class KeycloakJwtAuthenticationConverterTest {

  private final KeycloakJwtAuthenticationConverter converter =
      new KeycloakJwtAuthenticationConverter("task-manager-api");

  @Test
  void convertIncludesDefaultAndKeycloakRoles() {
    Jwt jwt = Jwt.withTokenValue("token")
        .header("alg", "none")
        .subject("subject-user")
        .claim("email", "user@example.com")
        .claim("scope", "profile")
        .claim("realm_access", Map.of("roles", List.of("user", " ROLE_admin ", " ")))
        .claim(
            "resource_access",
            Map.of(
                "task-manager-api", Map.of("roles", List.of("ADMIN", "role_user", "  ")),
                "another-client", Map.of("roles", List.of("MANAGER"))
            )
        )
        .build();

    AbstractAuthenticationToken authentication = converter.convert(jwt);

    assertThat(authentication.getName()).isEqualTo("user@example.com");
    assertThat(authorities(authentication))
        .contains("SCOPE_profile", "ROLE_USER", "ROLE_ADMIN")
        .doesNotContain("ROLE_MANAGER");
  }

  @Test
  void convertFallsBackToSubjectWhenEmailMissing() {
    Jwt jwt = Jwt.withTokenValue("token")
        .header("alg", "none")
        .subject("subject-user")
        .build();

    AbstractAuthenticationToken authentication = converter.convert(jwt);

    assertThat(authentication.getName()).isEqualTo("subject-user");
  }

  @Test
  void convertFallsBackToSubjectWhenEmailBlank() {
    Jwt jwt = Jwt.withTokenValue("token")
        .header("alg", "none")
        .subject("subject-user")
        .claim("email", "   ")
        .build();

    AbstractAuthenticationToken authentication = converter.convert(jwt);

    assertThat(authentication.getName()).isEqualTo("subject-user");
  }

  @Test
  void convertIgnoresMalformedRoleClaims() {
    Jwt jwt = Jwt.withTokenValue("token")
        .header("alg", "none")
        .subject("subject-user")
        .claim("realm_access", "invalid")
        .claim("resource_access", Map.of("task-manager-api", "invalid"))
        .build();

    AbstractAuthenticationToken authentication = converter.convert(jwt);

    assertThat(authorities(authentication).stream().noneMatch(authority -> authority.startsWith("ROLE_")))
        .isTrue();
  }

  private Set<String> authorities(AbstractAuthenticationToken authentication) {
    return authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .collect(Collectors.toSet());
  }
}
