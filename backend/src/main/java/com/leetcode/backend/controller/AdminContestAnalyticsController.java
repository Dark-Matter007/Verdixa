package com.leetcode.backend.controller;

import com.leetcode.backend.dto.AdminContestAnalyticsResponse;
import com.leetcode.backend.dto.AdminContestDeleteResponse;
import com.leetcode.backend.dto.AdminContestDetailResponse;
import com.leetcode.backend.dto.AdminContestListResponse;
import com.leetcode.backend.service.ContestService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Kept under /api/admin so the existing admin security rule protects participant data. */
@RestController
@RequestMapping("/api/admin/contests")
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class AdminContestAnalyticsController {
    private final ContestService contestService;

    public AdminContestAnalyticsController(ContestService contestService) { this.contestService = contestService; }

    @GetMapping("/{id}/analytics")
    public AdminContestAnalyticsResponse analytics(@PathVariable Long id) { return contestService.adminAnalytics(id); }

    @GetMapping
    public List<AdminContestListResponse> list() { return contestService.adminContestList(); }

    @GetMapping("/{id}")
    public AdminContestDetailResponse detail(@PathVariable @Positive Long id) {
        return contestService.adminContestDetail(id);
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{id}")
    public ResponseEntity<AdminContestDeleteResponse> delete(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(contestService.deleteAdminContest(id));
    }
}
