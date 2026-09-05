package com.leetcode.backend.repository;

import com.leetcode.backend.model.DailyChallenge;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyChallengeRepository extends JpaRepository<DailyChallenge, Long> {

    /** A daily response always renders its problem details, so fetch that relation
     * in the repository query instead of relying on the web request session. */
    @EntityGraph(attributePaths = "problem")
    Optional<DailyChallenge> findByChallengeDate(LocalDate date);

    @EntityGraph(attributePaths = "problem")
    List<DailyChallenge> findByChallengeDateBetweenOrderByChallengeDateDesc(LocalDate from, LocalDate to);

    @EntityGraph(attributePaths = "problem")
    List<DailyChallenge> findAllByOrderByChallengeDateDesc();
}
