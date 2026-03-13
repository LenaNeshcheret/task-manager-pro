package com.lenaneshcheret.taskmanager.service;

import com.lenaneshcheret.taskmanager.domain.User;
import com.lenaneshcheret.taskmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

  private final UserRepository userRepository;

  @Transactional
  public User getOrCreateCurrentUser(JwtAuthenticationToken authentication) {
    String identifier = resolveIdentifier(authentication);
    return userRepository.findByEmail(identifier)
        .orElseGet(() -> userRepository.save(User.fromExternalIdentity(identifier)));
  }

  private String resolveIdentifier(JwtAuthenticationToken authentication) {
    Jwt jwt = authentication.getToken();

    String email = jwt.getClaimAsString("email");
    if (email != null && !email.isBlank()) {
      return email;
    }

    String preferredUsername = jwt.getClaimAsString("preferred_username");
    if (preferredUsername != null && !preferredUsername.isBlank()) {
      return preferredUsername;
    }

    return authentication.getName();
  }
}
