package com.leetcode.backend.repository;
import com.leetcode.backend.model.AssessmentCreatorDocument; import org.springframework.data.jpa.repository.JpaRepository; import java.util.Optional;
public interface AssessmentCreatorDocumentRepository extends JpaRepository<AssessmentCreatorDocument,Long>{Optional<AssessmentCreatorDocument> findByApplicationId(Long applicationId);}
