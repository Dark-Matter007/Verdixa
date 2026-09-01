package com.leetcode.backend.service;

import com.leetcode.backend.model.Problem;
import com.leetcode.backend.model.ProblemBookmark;
import com.leetcode.backend.model.User;
import com.leetcode.backend.repository.ProblemBookmarkRepository;
import com.leetcode.backend.repository.ProblemRepository;
import com.leetcode.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ProblemBookmarkService {
    private final ProblemBookmarkRepository bookmarkRepository;
    private final ProblemRepository problemRepository;
    private final UserRepository userRepository;
    public ProblemBookmarkService(ProblemBookmarkRepository bookmarkRepository, ProblemRepository problemRepository, UserRepository userRepository) {
        this.bookmarkRepository = bookmarkRepository; this.problemRepository = problemRepository; this.userRepository = userRepository;
    }
    @Transactional
    public void add(String username, Long problemId) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        Problem problem = problemRepository.findById(problemId).orElseThrow(() -> new RuntimeException("Problem not found"));
        if (!problem.isActive()) throw new RuntimeException("Problem not found");
        if (bookmarkRepository.existsByUserIdAndProblemId(user.getId(), problemId)) return;
        ProblemBookmark bookmark = new ProblemBookmark(); bookmark.setUser(user); bookmark.setProblem(problem); bookmarkRepository.save(bookmark);
    }
    @Transactional
    public void remove(String username, Long problemId) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        bookmarkRepository.findByUserIdAndProblemId(user.getId(), problemId).ifPresent(bookmarkRepository::delete);
    }
    @Transactional(readOnly = true)
    public List<Problem> list(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        return bookmarkRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream().map(ProblemBookmark::getProblem).filter(Problem::isActive).toList();
    }
    @Transactional(readOnly = true)
    public boolean isBookmarked(String username, Long problemId) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        return bookmarkRepository.existsByUserIdAndProblemId(user.getId(), problemId);
    }
}
