package com.leetcode.backend.controller;

import com.leetcode.backend.dto.AdminStatisticsResponse;
import com.leetcode.backend.model.Role;
import com.leetcode.backend.repository.ProblemRepository;
import com.leetcode.backend.repository.SubmissionRepository;
import com.leetcode.backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import java.time.LocalDate;
import java.util.*;
import com.leetcode.backend.dto.PageResponse;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final UserRepository userRepository;
    private final ProblemRepository problemRepository;
    private final SubmissionRepository submissionRepository;

    public AdminController(UserRepository userRepository, ProblemRepository problemRepository,
                           SubmissionRepository submissionRepository) {
        this.userRepository = userRepository;
        this.problemRepository = problemRepository;
        this.submissionRepository = submissionRepository;
    }

    @GetMapping("/statistics")
    public ResponseEntity<AdminStatisticsResponse> getStatistics() {
        long totalSubmissions = submissionRepository.count();
        long accepted = submissionRepository.countByStatus("ACCEPTED");
        long runtimeErrors = submissionRepository.countByStatus("RUNTIME_ERROR")
                + submissionRepository.countByStatus("EXECUTION_ERROR");
        return ResponseEntity.ok(new AdminStatisticsResponse(
                userRepository.count(),
                userRepository.countByRole(Role.USER),
                userRepository.countByRole(Role.ADMIN),
                problemRepository.count(), problemRepository.countByActiveTrue(), problemRepository.countByActiveFalse(),
                totalSubmissions, accepted,
                submissionRepository.countByStatus("WRONG_ANSWER"),
                submissionRepository.countByStatus("COMPILATION_ERROR"), runtimeErrors,
                submissionRepository.countByStatus("TIME_LIMIT_EXCEEDED"),
                totalSubmissions == 0 ? 0 : (int) Math.round(accepted * 100.0 / totalSubmissions)));
    }

    /** Aggregate-only administration view; no source, credentials, notes, or hidden test values are returned. */
    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> analytics() {
        var all = submissionRepository.findAll();
        long accepted = all.stream().filter(s -> "ACCEPTED".equals(s.getStatus())).count();
        LocalDate today = LocalDate.now();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalUsers", userRepository.count());
        // Calendar windows include today, so a 30-day window starts 29 days ago.
        result.put("activeUsers", all.stream().filter(s -> !s.getSubmittedAt().toLocalDate().isBefore(today.minusDays(29))).map(s -> s.getUser().getId()).distinct().count());
        result.put("publishedProblems", problemRepository.countByActiveTrue());
        result.put("totalSubmissions", all.size()); result.put("acceptedSubmissions", accepted);
        result.put("acceptanceRate", all.isEmpty() ? 0 : Math.round(accepted * 100.0 / all.size()));
        result.put("submissionsToday", all.stream().filter(s -> s.getSubmittedAt().toLocalDate().equals(today)).count());
        result.put("submissionsLast7Days", all.stream().filter(s -> !s.getSubmittedAt().toLocalDate().isBefore(today.minusDays(6))).count());
        result.put("submissionsLast30Days", all.stream().filter(s -> !s.getSubmittedAt().toLocalDate().isBefore(today.minusDays(29))).count());
        result.put("verdictDistribution", all.stream().collect(java.util.stream.Collectors.groupingBy(s -> s.getStatus(), java.util.stream.Collectors.counting())));
        result.put("languageDistribution", all.stream().collect(java.util.stream.Collectors.groupingBy(s -> s.getLanguage(), java.util.stream.Collectors.counting())));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/activity/page")
    public ResponseEntity<PageResponse<Map<String, Object>>> recentActivity(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<Map<String, Object>> values = submissionRepository.findAll().stream()
                .sorted(Comparator.comparing(s -> s.getSubmittedAt(), Comparator.reverseOrder()))
                .map(s -> Map.<String, Object>of("id", s.getId(), "username", s.getUser().getUsername(),
                        "problemTitle", s.getProblem().getTitle(), "status", s.getStatus(), "language", s.getLanguage(), "submittedAt", s.getSubmittedAt()))
                .toList();
        return ResponseEntity.ok(PageResponse.from(values, page, size));
    }
}
