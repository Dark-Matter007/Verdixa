package com.leetcode.backend.controller;

import com.leetcode.backend.model.Problem;
import com.leetcode.backend.service.ProblemBookmarkService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/bookmarks")
public class ProblemBookmarkController {
    private final ProblemBookmarkService bookmarkService;
    public ProblemBookmarkController(ProblemBookmarkService bookmarkService) { this.bookmarkService = bookmarkService; }
    @GetMapping public ResponseEntity<List<Problem>> list(Authentication authentication) { return ResponseEntity.ok(bookmarkService.list(authentication.getName())); }
    @GetMapping("/{problemId}") public ResponseEntity<Boolean> status(@PathVariable Long problemId, Authentication authentication) { return ResponseEntity.ok(bookmarkService.isBookmarked(authentication.getName(), problemId)); }
    @PostMapping("/{problemId}") public ResponseEntity<Void> add(@PathVariable Long problemId, Authentication authentication) { bookmarkService.add(authentication.getName(), problemId); return ResponseEntity.noContent().build(); }
    @DeleteMapping("/{problemId}") public ResponseEntity<Void> remove(@PathVariable Long problemId, Authentication authentication) { bookmarkService.remove(authentication.getName(), problemId); return ResponseEntity.noContent().build(); }
}
