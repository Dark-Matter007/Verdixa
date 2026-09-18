package com.leetcode.backend.repository;
import com.leetcode.backend.model.*; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import java.util.*;
public interface AssessmentInvitationRepository extends JpaRepository<AssessmentInvitation,Long>{
 @EntityGraph(attributePaths="user") @Query("select invitation from AssessmentInvitation invitation where invitation.assessment.id=:assessmentId") List<AssessmentInvitation> findForAccessByAssessmentId(@Param("assessmentId") Long assessmentId);
 boolean existsByAssessmentIdAndUserId(Long assessmentId,Long userId); Optional<AssessmentInvitation> findByAssessmentIdAndUserId(Long assessmentId,Long userId); List<AssessmentInvitation> findByAssessmentId(Long assessmentId); long countByAssessmentIdAndStatus(Long assessmentId,AssessmentEnums.InvitationStatus status); void deleteByAssessmentIdAndUserId(Long assessmentId,Long userId); void deleteByAssessmentId(Long assessmentId);
}
