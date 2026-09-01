package com.leetcode.backend.repository;

import com.leetcode.backend.model.Submission;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubmissionRepository
        extends JpaRepository<Submission, Long> {

    @EntityGraph(attributePaths = {"user", "problem"})
    List<Submission> findByUserIdOrderBySubmittedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"user", "problem"})
    List<Submission> findByProblemIdOrderBySubmittedAtDesc(Long problemId);

    @EntityGraph(attributePaths = {"user", "problem"})
    List<Submission> findByUserIdAndProblemIdOrderBySubmittedAtDesc(
            Long userId,
            Long problemId
    );

    List<Submission> findByStatus(String status);

    @EntityGraph(attributePaths = {"user", "problem"})
    java.util.Optional<Submission> findWithUserAndProblemById(Long id);

    long countByStatus(String status);
}
