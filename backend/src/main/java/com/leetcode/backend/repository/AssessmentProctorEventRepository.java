package com.leetcode.backend.repository;
import com.leetcode.backend.model.AssessmentProctorEvent; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface AssessmentProctorEventRepository extends JpaRepository<AssessmentProctorEvent,Long>{List<AssessmentProctorEvent> findBySessionIdOrderByOccurredAtAsc(Long sessionId); long countBySessionAssessmentId(Long assessmentId);}
