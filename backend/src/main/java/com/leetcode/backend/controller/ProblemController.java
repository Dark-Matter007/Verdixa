package com.leetcode.backend.controller;

import com.leetcode.backend.model.Problem;
import com.leetcode.backend.service.ProblemService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.leetcode.backend.dto.PageResponse;
import com.leetcode.backend.dto.ProblemDetailResponse;

@RestController
@RequestMapping("/api/problems")
public class ProblemController {

    private final ProblemService problemService;

    public ProblemController(ProblemService problemService) {
        this.problemService = problemService;
    }

    // Get all active problems
    @GetMapping
    public ResponseEntity<List<Problem>> getAllProblems() {
        return ResponseEntity.ok(problemService.getAllProblems());
    }

    // ADMIN ONLY (secured in SecurityConfig): includes inactive problems.
    @GetMapping("/admin/all")
    public ResponseEntity<List<Problem>> getAllProblemsForAdmin() {
        return ResponseEntity.ok(problemService.getAllProblemsForAdmin());
    }

    @GetMapping("/admin/page")
    public ResponseEntity<PageResponse<Problem>> getAdminProblemsPage(@RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        List<Problem> values = problemService.getAllProblemsForAdmin().stream()
                .filter(p -> search.isBlank() || p.getTitle().toLowerCase().contains(search.toLowerCase())).toList();
        return ResponseEntity.ok(PageResponse.from(values, page, size));
    }

    // Get problem by ID
    @GetMapping("/{id}")
    public ResponseEntity<ProblemDetailResponse> getProblemById(@PathVariable Long id, Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        return ResponseEntity.ok(problemService.getProblemDetail(id, isAdmin));
    }

    // Search problems by title
    @GetMapping("/search")
    public ResponseEntity<List<Problem>> searchProblems(
            @RequestParam String title) {
        return ResponseEntity.ok(problemService.searchProblems(title));
    }

    // Filter problems by difficulty
    @GetMapping("/difficulty/{difficulty}")
    public ResponseEntity<List<Problem>> getProblemsByDifficulty(
            @PathVariable String difficulty) {
        return ResponseEntity.ok(
                problemService.getProblemsByDifficulty(difficulty)
        );
    }

    // Create a new problem
    @PostMapping
    public ResponseEntity<Problem> createProblem(
            @RequestBody Problem problem) {

        Problem createdProblem = problemService.createProblem(problem);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdProblem);
    }

    // Update a problem
    @PutMapping("/{id}")
    public ResponseEntity<Problem> updateProblem(
            @PathVariable Long id,
            @RequestBody Problem problem) {

        return ResponseEntity.ok(
                problemService.updateProblem(id, problem)
        );
    }

    // Soft delete a problem
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProblem(
            @PathVariable Long id) {

        problemService.deleteProblem(id);

        return ResponseEntity.noContent().build();
    }
}
