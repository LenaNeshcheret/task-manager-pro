package com.lenaneshcheret.taskmanager.repository;

import com.lenaneshcheret.taskmanager.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByEmail(String email);
}
