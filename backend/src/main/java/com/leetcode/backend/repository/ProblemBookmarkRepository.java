package com.leetcode.backend.repository;

import com.leetcode.backend.model.ProblemBookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProblemBookmarkRepository extends JpaRepository<ProblemBookmark, Long> {
    List<ProblemBookmark> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<ProblemBookmark> findByUserIdAndProblemId(Long userId, Long problemId);
    boolean existsByUserIdAndProblemId(Long userId, Long problemId);
}
