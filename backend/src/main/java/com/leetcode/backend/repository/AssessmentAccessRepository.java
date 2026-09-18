package com.leetcode.backend.repository;

import com.leetcode.backend.model.AssessmentAccess;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
import java.util.List;

public interface AssessmentAccessRepository extends JpaRepository<AssessmentAccess, Long> {
    @EntityGraph(attributePaths = {"assessment", "assessment.creator", "invitation", "invitation.user", "participant"})
    @Query("select distinct access from AssessmentAccess access")
    List<AssessmentAccess> findAllForGrantValidation();
    Optional<AssessmentAccess> findByAssessmentIdAndEmailIgnoreCase(Long assessmentId, String email);
    Optional<AssessmentAccess> findByIdAndAssessmentId(Long id, Long assessmentId);
    long countByAssessmentIdAndVerifiedAtIsNotNull(Long assessmentId);
    List<AssessmentAccess> findByAssessmentId(Long assessmentId);
    List<AssessmentAccess> findByAssessmentIdAndVerifiedAtIsNotNull(Long assessmentId);
}
