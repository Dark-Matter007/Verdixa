package com.leetcode.backend.repository;

import com.leetcode.backend.model.ContestProblem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface ContestProblemRepository extends JpaRepository<ContestProblem,Long>{
 List<ContestProblem> findByContestIdOrderByDisplayOrderAsc(Long contestId);
 @Query("select cp from ContestProblem cp join fetch cp.problem where cp.contest.id = :contestId order by cp.displayOrder asc")
 List<ContestProblem> findWithProblemByContestIdOrderByDisplayOrderAsc(@Param("contestId") Long contestId);
 Optional<ContestProblem> findByContestIdAndProblemId(Long contestId,Long problemId);
 @Modifying
 @Query("delete from ContestProblem cp where cp.contest.id = :contestId")
 int deleteAllByContestId(@Param("contestId") Long contestId);
}
