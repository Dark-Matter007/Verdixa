package com.leetcode.backend.repository;

import com.leetcode.backend.model.EditorialReveal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EditorialRevealRepository extends JpaRepository<EditorialReveal, Long> {
    boolean existsByUserIdAndProblemId(Long userId, Long problemId);
    long countByUserIdAndProblemId(Long userId, Long problemId);
}
