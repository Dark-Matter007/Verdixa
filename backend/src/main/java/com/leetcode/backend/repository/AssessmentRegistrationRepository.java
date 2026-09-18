package com.leetcode.backend.repository;
import com.leetcode.backend.model.AssessmentRegistration; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface AssessmentRegistrationRepository extends JpaRepository<AssessmentRegistration,Long>{boolean existsByAssessmentIdAndUserId(Long assessmentId,Long userId); long countByAssessmentId(Long assessmentId); List<AssessmentRegistration> findByAssessmentId(Long assessmentId);}
