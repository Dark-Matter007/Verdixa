package com.leetcode.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

@Entity
@Table(name = "users")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    /** Null means this account has no local password credential (OAuth-only). */
    @Column
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, columnDefinition = "varchar(10) default 'LIGHT'")
    private ThemePreference theme = ThemePreference.LIGHT;

    /* Defaults to true so Hibernate's update migration preserves pre-verification accounts. */
    @Column(name = "email_verified", nullable = false, columnDefinition = "boolean default true")
    private boolean emailVerified = true;

    /** Incremented after identity or credential changes so previously issued JWTs stop working. */
    @Column(name = "auth_version", nullable = false, columnDefinition = "bigint default 0")
    private long authVersion = 0;

    public User() {
    }

    public User(String username, String email, String password, Role role) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public ThemePreference getTheme() { return theme; }

    public void setTheme(ThemePreference theme) { this.theme = theme; }

    public boolean isEmailVerified() { return emailVerified; }

    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public long getAuthVersion() { return authVersion; }

    public void setAuthVersion(long authVersion) { this.authVersion = authVersion; }

    public void incrementAuthVersion() { this.authVersion++; }
}
