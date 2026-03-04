package com.lenaneshcheret.taskmanager.controller;

import java.util.Set;
import java.util.TreeSet;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class MeController {

  @GetMapping("/me")
  public MeResponse me(JwtAuthenticationToken authentication) {
    Jwt jwt = authentication.getToken();
    String email = jwt.getClaimAsString("email");
    if (email == null || email.isBlank()) {
      email = authentication.getName();
    }

    Set<String> roles = authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .filter(authority -> authority.startsWith("ROLE_"))
        .collect(TreeSet::new, TreeSet::add, TreeSet::addAll);

    return new MeResponse(email, roles);
  }

  public record MeResponse(String email, Set<String> roles) {
  }
}
