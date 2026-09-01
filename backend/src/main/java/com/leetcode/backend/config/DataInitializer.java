package com.leetcode.backend.config;

import com.leetcode.backend.model.Role;
import com.leetcode.backend.model.User;
import com.leetcode.backend.repository.UserRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    @ConditionalOnProperty(prefix = "algosphere.bootstrap-admin", name = "password")
    CommandLineRunner createAdmin(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${algosphere.bootstrap-admin.username}") String username,
            @Value("${algosphere.bootstrap-admin.password}") String password) {

        return args -> {

            if (!username.isBlank() && !password.isBlank() && !userRepository.existsByUsername(username)) {

                User admin = new User();

                admin.setUsername(username);
                admin.setEmail(username + "@algosphere.local");

                admin.setPassword(
                        passwordEncoder.encode(password)
                );

                admin.setRole(Role.ADMIN);

                userRepository.save(admin);

                System.out.println("Verdixa bootstrap administrator created.");
            }
        };
    }
}
