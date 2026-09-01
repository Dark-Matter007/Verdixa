package com.leetcode.backend.repository;

import com.leetcode.backend.model.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestCaseRepository extends JpaRepository<TestCase, Long> {

    List<TestCase> findByProblemId(Long problemId);

    List<TestCase> findByProblemIdAndHiddenFalse(Long problemId);

    long countByProblemIdAndHiddenTrue(Long problemId);
}
