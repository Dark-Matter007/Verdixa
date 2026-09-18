package com.leetcode.backend.repository;
import com.leetcode.backend.model.*; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import java.util.*; import java.time.*;
public interface AssessmentRepository extends JpaRepository<Assessment,Long>{
 @EntityGraph(attributePaths="creator") @Query("select a from Assessment a where a.id=:id") Optional<Assessment> findPreflightById(@Param("id") Long id);
 List<Assessment> findByCreatorIdOrderByCreatedAtDesc(Long creatorId); List<Assessment> findByVisibilityAndStatusNotOrderByStartAtAsc(AssessmentEnums.Visibility visibility,AssessmentEnums.Status excluded); List<Assessment> findByStartAtBetween(LocalDateTime from,LocalDateTime to);
}
