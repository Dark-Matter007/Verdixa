package com.leetcode.backend.dto;

import com.leetcode.backend.model.Role;
import com.leetcode.backend.model.ThemePreference;
import com.leetcode.backend.model.User;

public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private Role role;
    private ThemePreference theme;
    private String languagePreference;

    public UserResponse() {
    }

    public UserResponse(Long id, String username, String email, Role role, ThemePreference theme, String languagePreference) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.theme = theme;
        this.languagePreference = languagePreference;
    }

    public static UserResponse fromUser(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getTheme(), user.getLanguagePreference()
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

    public ThemePreference getTheme() { return theme; }
    public String getLanguagePreference() { return languagePreference; }
}
