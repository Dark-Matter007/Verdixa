package com.leetcode.backend.controller;

import com.leetcode.backend.dto.DailyChallengeAdminResponse;
import com.leetcode.backend.dto.DailyChallengeResponse;
import com.leetcode.backend.model.DailyChallenge;
import com.leetcode.backend.service.DailyChallengeService;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/daily-challenges")
public class DailyChallengeController {
    private final DailyChallengeService service;

    public DailyChallengeController(DailyChallengeService service) { this.service = service; }

    @GetMapping("/today")
    public ResponseEntity<DailyChallengeResponse> today(Authentication authentication) {
        return service.today(authentication.getName()).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping
    public List<DailyChallengeResponse> history(Authentication authentication) {
        return service.history(authentication.getName());
    }

    @GetMapping("/summary")
    public Map<String, Integer> summary(Authentication authentication) {
        return service.summary(authentication.getName());
    }

    @GetMapping("/admin")
    public List<DailyChallengeAdminResponse> adminHistory(Authentication authentication) {
        requireAdmin(authentication);
        return service.adminHistory();
    }

    @PostMapping
    public DailyChallengeResponse schedule(@RequestBody DailyChallenge challenge, Authentication authentication) {
        requireAdmin(authentication);
        return service.schedule(challenge);
    }

    @PutMapping("/{id}")
    public DailyChallengeResponse updateFuture(@PathVariable Long id, @RequestBody DailyChallenge challenge,
            Authentication authentication) {
        requireAdmin(authentication);
        return service.updateFuture(id, challenge);
    }

    @DeleteMapping("/{id}")
    public void deleteFuture(@PathVariable Long id, Authentication authentication) {
        requireAdmin(authentication);
        service.deleteFuture(id);
    }

    private void requireAdmin(Authentication authentication) {
        if (authentication.getAuthorities().stream().noneMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) {
            throw new AccessDeniedException("Admin required.");
        }
    }
}
