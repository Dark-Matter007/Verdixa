package com.leetcode.backend.controller;

import com.leetcode.backend.dto.LeaderboardEntryResponse;
import com.leetcode.backend.dto.UserProgressResponse;
import com.leetcode.backend.dto.UserResponse;
import com.leetcode.backend.model.Role;
import com.leetcode.backend.service.UserService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.leetcode.backend.dto.PageResponse;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        UserProgressResponse progress = userService
                .getProgressForUsername(authentication.getName());

        return ResponseEntity.ok(new UserResponse(
                progress.getId(),
                progress.getUsername(),
                progress.getEmail(),
                progress.getRole()));
    }

    @GetMapping("/me/progress")
    public ResponseEntity<UserProgressResponse> getCurrentUserProgress(
            Authentication authentication) {

        return ResponseEntity.ok(
                userService.getProgressForUsername(authentication.getName())
        );
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<LeaderboardEntryResponse>> getLeaderboard() {
        return ResponseEntity.ok(userService.getLeaderboard());
    }

    @GetMapping("/leaderboard/page")
    public ResponseEntity<PageResponse<LeaderboardEntryResponse>> getLeaderboardPage(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.from(userService.getLeaderboard(), page, size));
    }

    // Get all users
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {

        return ResponseEntity.ok(
                userService.getAllUsers()
        );
    }

    @GetMapping("/page")
    public ResponseEntity<PageResponse<UserResponse>> getUsersPage(@RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.from(search.isBlank() ? userService.getAllUsers() : userService.searchUsers(search), page, size));
    }

    // Search users
    @GetMapping("/search")
    public ResponseEntity<List<UserResponse>> searchUsers(
            @RequestParam String query) {

        return ResponseEntity.ok(
                userService.searchUsers(query)
        );
    }

    // Get user by ID
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                userService.getUserById(id)
        );
    }

    // Change user role
    @PutMapping("/{id}/role")
    public ResponseEntity<UserResponse> updateRole(
            @PathVariable Long id,
            @RequestParam Role role) {

        return ResponseEntity.ok(
                userService.updateRole(id, role)
        );
    }
}
