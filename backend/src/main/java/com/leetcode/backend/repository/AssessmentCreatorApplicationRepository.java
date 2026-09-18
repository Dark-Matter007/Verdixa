package com.leetcode.backend.repository;
import com.leetcode.backend.model.*; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface AssessmentCreatorApplicationRepository extends JpaRepository<AssessmentCreatorApplication,Long>{Optional<AssessmentCreatorApplication> findTopByUserIdOrderByIdDesc(Long userId); boolean existsByUserIdAndStatus(Long userId,AssessmentEnums.CreatorStatus status); List<AssessmentCreatorApplication> findByStatusOrderBySubmittedAtAsc(AssessmentEnums.CreatorStatus status);}
