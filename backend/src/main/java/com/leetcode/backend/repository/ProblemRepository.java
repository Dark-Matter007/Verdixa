package com.leetcode.backend.repository;

import com.leetcode.backend.model.Problem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProblemRepository extends JpaRepository<Problem, Long> {

    List<Problem> findByDifficultyIgnoreCase(String difficulty);

    List<Problem> findByActiveTrue();

    List<Problem> findByActiveTrueOrderByIdAsc();

    long countByActiveTrue();

    long countByActiveFalse();

    List<Problem> findByTitleContainingIgnoreCase(String title);

    @EntityGraph(attributePaths = {"functionSignature", "functionSignature.parameters"})
    Optional<Problem> findWithFunctionSignatureById(Long id);
}
