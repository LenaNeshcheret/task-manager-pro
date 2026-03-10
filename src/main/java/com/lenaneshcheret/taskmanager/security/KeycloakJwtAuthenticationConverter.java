package com.lenaneshcheret.taskmanager.security;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

  private final JwtGrantedAuthoritiesConverter defaultAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
  private final String clientId;

  public KeycloakJwtAuthenticationConverter(String clientId) {
    this.clientId = clientId;
  }

  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {
    Set<GrantedAuthority> authorities = new HashSet<>();

    Collection<GrantedAuthority> defaultAuthorities = defaultAuthoritiesConverter.convert(jwt);
    if (defaultAuthorities != null) {
      authorities.addAll(defaultAuthorities);
    }

    extractRealmRoles(jwt).stream()
        .map(this::toRoleAuthority)
        .filter(authority -> !authority.isBlank())
        .map(SimpleGrantedAuthority::new)
        .forEach(authorities::add);

    extractClientRoles(jwt).stream()
        .map(this::toRoleAuthority)
        .filter(authority -> !authority.isBlank())
        .map(SimpleGrantedAuthority::new)
        .forEach(authorities::add);

    return new JwtAuthenticationToken(jwt, authorities, resolvePrincipalName(jwt));
  }

  private String resolvePrincipalName(Jwt jwt) {
    String email = jwt.getClaimAsString("email");
    if (email != null && !email.isBlank()) {
      return email;
    }

    String preferredUsername = jwt.getClaimAsString("preferred_username");
    if (preferredUsername != null && !preferredUsername.isBlank()) {
      return preferredUsername;
    }

    return jwt.getSubject();
  }

  @SuppressWarnings("unchecked")
  private Collection<String> extractRealmRoles(Jwt jwt) {
    Object realmAccessClaim = jwt.getClaim("realm_access");
    if (!(realmAccessClaim instanceof Map<?, ?> realmAccess)) {
      return Collections.emptySet();
    }

    Object rolesClaim = realmAccess.get("roles");
    if (!(rolesClaim instanceof Collection<?> roles)) {
      return Collections.emptySet();
    }

    return roles.stream()
        .filter(String.class::isInstance)
        .map(String.class::cast)
        .toList();
  }

  @SuppressWarnings("unchecked")
  private Collection<String> extractClientRoles(Jwt jwt) {
    Object resourceAccessClaim = jwt.getClaim("resource_access");
    if (!(resourceAccessClaim instanceof Map<?, ?> resourceAccess)) {
      return Collections.emptySet();
    }

    Object clientAccessClaim = resourceAccess.get(clientId);
    if (!(clientAccessClaim instanceof Map<?, ?> clientAccess)) {
      return Collections.emptySet();
    }

    Object rolesClaim = clientAccess.get("roles");
    if (!(rolesClaim instanceof Collection<?> roles)) {
      return Collections.emptySet();
    }

    return roles.stream()
        .filter(String.class::isInstance)
        .map(String.class::cast)
        .toList();
  }

  private String toRoleAuthority(String role) {
    String normalizedRole = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
    if (normalizedRole.isBlank()) {
      return "";
    }
    if (normalizedRole.startsWith("ROLE_")) {
      return normalizedRole;
    }
    return "ROLE_" + normalizedRole;
  }
}
