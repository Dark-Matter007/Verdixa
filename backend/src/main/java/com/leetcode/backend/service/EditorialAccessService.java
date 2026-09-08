package com.leetcode.backend.service;

import com.leetcode.backend.dto.*;
import com.leetcode.backend.model.*;
import com.leetcode.backend.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.Set;

@Service
public class EditorialAccessService {
    // Hints count official compilation attempts too. Infrastructure errors are excluded.
    private static final Set<String> FAILURES = Set.of("WRONG_ANSWER", "TIME_LIMIT_EXCEEDED", "RUNTIME_ERROR", "MEMORY_LIMIT_EXCEEDED", "COMPILATION_ERROR");
    private final EditorialRepository editorials;
    private final EditorialRevealRepository reveals;
    private final UserRepository users;
    private final SubmissionRepository submissions;
    public EditorialAccessService(EditorialRepository editorials, EditorialRevealRepository reveals, UserRepository users, SubmissionRepository submissions) {
        this.editorials = editorials; this.reveals = reveals; this.users = users; this.submissions = submissions;
    }
    @Transactional(readOnly = true)
    public EditorialAccessResponse access(Long problemId, String username) { return state(problemId, user(username)); }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public EditorialAccessResponse reveal(Long problemId, String username) {
        User user = users.lockById(user(username).getId()).orElseThrow();
        EditorialAccessResponse state = state(problemId, user);
        if (!state.published() || state.status().equals("LOCKED"))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solve this problem or complete three qualifying failed submissions before revealing its editorial.");
        if (!reveals.existsByUserIdAndProblemId(user.getId(), problemId))
            reveals.saveAndFlush(new EditorialReveal(user, editorials.findByProblemId(problemId).orElseThrow().getProblem()));
        return state(problemId, user);
    }
    private EditorialAccessResponse state(Long problemId, User user) {
        Editorial e = editorials.findByProblemId(problemId).orElse(null);
        boolean published = e != null && e.isPublished();
        long failed = submissions.countByUserIdAndProblemIdAndStatusIn(user.getId(), problemId, FAILURES);
        boolean accepted = submissions.existsByUserIdAndProblemIdAndStatus(user.getId(), problemId, "ACCEPTED");
        boolean revealed = published && reveals.existsByUserIdAndProblemId(user.getId(), problemId);
        String status = revealed ? "REVEALED" : published && (accepted || failed >= 3) ? "AVAILABLE" : "LOCKED";
        return new EditorialAccessResponse(status, failed, 3, Math.max(0, 3 - failed), accepted, published,
                revealed ? EditorialResponse.from(e) : null);
    }
    private User user(String username) { return users.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED)); }
}
