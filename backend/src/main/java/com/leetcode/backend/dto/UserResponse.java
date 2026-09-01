package com.leetcode.backend.dto;

import com.leetcode.backend.model.Role;
import com.leetcode.backend.model.User;

public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private Role role;

    public UserResponse() {
    }

    public UserResponse(Long id, String username, String email, Role role) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
    }

    public static UserResponse fromUser(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole()
        );
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }
}