package com.leetcode.backend.repository;

import com.leetcode.backend.model.Submission;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    long countByUserIdAndProblemId(Long userId, Long problemId);

    @EntityGraph(attributePaths = {"user", "problem", "contest", "contestProblem"})
    List<Submission> findByContestIdOrderBySubmittedAtAsc(Long contestId);

    @EntityGraph(attributePaths = {"user", "problem", "contest", "contestProblem"})
    List<Submission> findByContestIdAndUserIdOrderBySubmittedAtDesc(Long contestId, Long userId);

    /** Aggregate library-card statistics in one query instead of loading rows per card. */
    @Query("""
            select s.problem.id as problemId,
                   count(s) as totalSubmissions,
                   coalesce(sum(case when s.status = 'ACCEPTED' then 1 else 0 end), 0) as acceptedSubmissions
            from Submission s
            where s.problem.id in :problemIds
            group by s.problem.id
            """)
    List<ProblemSubmissionStats> summarizeByProblemIds(@Param("problemIds") List<Long> problemIds);

    interface ProblemSubmissionStats {
        Long getProblemId();
        long getTotalSubmissions();
        long getAcceptedSubmissions();
    }
}
