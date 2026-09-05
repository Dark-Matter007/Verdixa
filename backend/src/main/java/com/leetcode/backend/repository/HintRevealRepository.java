package com.leetcode.backend.repository;
import com.leetcode.backend.model.HintReveal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface HintRevealRepository extends JpaRepository<HintReveal, Long> {
 List<HintReveal> findByUserIdAndProblemId(Long userId, Long problemId);
 Optional<HintReveal> findByUserIdAndHintId(Long userId, Long hintId);
}
