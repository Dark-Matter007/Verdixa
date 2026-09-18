package com.leetcode.backend.repository;

import com.leetcode.backend.model.AssessmentAccessAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssessmentAccessAuditRepository extends JpaRepository<AssessmentAccessAudit, Long> { }
