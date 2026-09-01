package com.leetcode.backend.repository;

import com.leetcode.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import com.leetcode.backend.model.Role;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    long countByRole(Role role);
}
