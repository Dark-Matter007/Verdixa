package com.leetcode.backend.controller;

import com.leetcode.backend.dto.SubmissionResponse;
import com.leetcode.backend.model.Submission;
import com.leetcode.backend.model.User;
import com.leetcode.backend.repository.UserRepository;
import com.leetcode.backend.service.SubmissionService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
import com.leetcode.backend.dto.PageResponse;

@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;
    private final UserRepository userRepository;

    public SubmissionController(
            SubmissionService submissionService,
            UserRepository userRepository) {

        this.submissionService = submissionService;
        this.userRepository = userRepository;
    }

    // =========================================================
    // CREATE SUBMISSION
    // =========================================================

    @PostMapping
    public ResponseEntity<SubmissionResponse> createSubmission(
            @RequestParam Long problemId,
            @RequestParam String language,
            @RequestParam(required = false) Long contestId,
            @RequestBody String sourceCode,
            Authentication authentication) {

        String username = authentication.getName();

        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Authenticated user not found."
                        )
                );

        Submission submission =
                submissionService.createSubmission(
                        user.getId(),
                        problemId,
                        language,
                        sourceCode,
                        contestId
                );

        // The judge service returns a saved entity. Reload its response graph so
        // serialization never depends on open-in-view or a detached lazy proxy.
        Submission responseSubmission = submissionService.getSubmissionById(submission.getId());

        SubmissionResponse response = new SubmissionResponse(responseSubmission);
        response.setCertificateProgress(submission.getCertificateProgress());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // =========================================================
    // GET SUBMISSION BY ID
    // =========================================================

    @GetMapping("/{id}")
    public ResponseEntity<SubmissionResponse> getSubmission(
            @PathVariable Long id,
            Authentication authentication) {

        Submission submission =
                submissionService.getSubmissionById(id);

        String username = authentication.getName();

        User authenticatedUser = userRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Authenticated user not found."
                        )
                );

        boolean isAdmin =
                authentication.getAuthorities()
                        .stream()
                        .anyMatch(
                                authority ->
                                        authority.getAuthority()
                                                .equals("ROLE_ADMIN")
                        );

        if (!isAdmin &&
                !authenticatedUser.getId()
                        .equals(submission.getUser().getId())) {

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .build();
        }

        return ResponseEntity.ok(
                new SubmissionResponse(submission)
        );
    }

    // =========================================================
    // GET USER SUBMISSIONS
    // =========================================================

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<SubmissionResponse>> getUserSubmissions(
            @PathVariable Long userId,
            Authentication authentication) {

        String username = authentication.getName();

        User authenticatedUser = userRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Authenticated user not found."
                        )
                );

        boolean isAdmin =
                authentication.getAuthorities()
                        .stream()
                        .anyMatch(
                                authority ->
                                        authority.getAuthority()
                                                .equals("ROLE_ADMIN")
                        );

        if (!isAdmin &&
                !authenticatedUser.getId().equals(userId)) {

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .build();
        }

        List<SubmissionResponse> responses =
                submissionService
                        .getUserSubmissions(userId)
                        .stream()
                        .map(SubmissionResponse::new)
                        .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/user/{userId}/page")
    public ResponseEntity<PageResponse<SubmissionResponse>> getUserSubmissionsPage(
            @PathVariable Long userId, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, Authentication authentication) {
        User current = userRepository.findByUsername(authentication.getName()).orElseThrow();
        boolean admin = authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (!admin && !current.getId().equals(userId)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        List<SubmissionResponse> values = submissionService.getUserSubmissions(userId).stream().map(SubmissionResponse::new).toList();
        return ResponseEntity.ok(PageResponse.from(values, page, size));
    }

    // =========================================================
    // GET PROBLEM SUBMISSIONS
    // =========================================================

    @GetMapping("/problem/{problemId}")
    public ResponseEntity<List<SubmissionResponse>> getProblemSubmissions(
            @PathVariable Long problemId,
            Authentication authentication) {
        User authenticatedUser = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found."));
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        List<SubmissionResponse> responses = (isAdmin
                ? submissionService.getProblemSubmissions(problemId)
                : submissionService.getUserProblemSubmissions(authenticatedUser.getId(), problemId))
                        .stream()
                        .map(SubmissionResponse::new)
                        .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    // =========================================================
    // GET USER + PROBLEM SUBMISSIONS
    // =========================================================

    @GetMapping("/user/{userId}/problem/{problemId}")
    public ResponseEntity<List<SubmissionResponse>>
    getUserProblemSubmissions(
            @PathVariable Long userId,
            @PathVariable Long problemId,
            Authentication authentication) {

        String username = authentication.getName();

        User authenticatedUser = userRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Authenticated user not found."
                        )
                );

        boolean isAdmin =
                authentication.getAuthorities()
                        .stream()
                        .anyMatch(
                                authority ->
                                        authority.getAuthority()
                                                .equals("ROLE_ADMIN")
                        );

        if (!isAdmin &&
                !authenticatedUser.getId().equals(userId)) {

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .build();
        }

        List<SubmissionResponse> responses =
                submissionService
                        .getUserProblemSubmissions(
                                userId,
                                problemId
                        )
                        .stream()
                        .map(SubmissionResponse::new)
                        .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }
}
