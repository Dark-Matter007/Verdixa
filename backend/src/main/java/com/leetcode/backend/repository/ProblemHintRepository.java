package com.leetcode.backend.repository;
import com.leetcode.backend.model.ProblemHint;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ProblemHintRepository extends JpaRepository<ProblemHint, Long> {
 List<ProblemHint> findByProblemIdOrderByDisplayOrderAsc(Long problemId);
 List<ProblemHint> findByProblemIdAndActiveTrueOrderByDisplayOrderAsc(Long problemId);
 long countByProblemIdAndActiveTrue(Long problemId);
}
