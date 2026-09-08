package com.leetcode.backend.repository;

import com.leetcode.backend.model.Problem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProblemRepository extends JpaRepository<Problem, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Problem p where p.id = :id")
    java.util.Optional<Problem> lockById(@org.springframework.data.repository.query.Param("id") Long id);


    List<Problem> findByDifficultyIgnoreCase(String difficulty);

    List<Problem> findByActiveTrue();

    List<Problem> findByActiveTrueOrderByIdAsc();

    long countByActiveTrue();

    long countByActiveFalse();

    List<Problem> findByTitleContainingIgnoreCase(String title);

    @EntityGraph(attributePaths = {"functionSignature", "functionSignature.parameters"})
    Optional<Problem> findWithFunctionSignatureById(Long id);
}
