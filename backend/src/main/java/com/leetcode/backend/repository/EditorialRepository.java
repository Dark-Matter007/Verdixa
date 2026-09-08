package com.leetcode.backend.repository; import com.leetcode.backend.model.Editorial; import org.springframework.data.jpa.repository.JpaRepository; import java.util.Optional; public interface EditorialRepository extends JpaRepository<Editorial,Long>{Optional<Editorial> findByProblemId(Long problemId);
@org.springframework.data.jpa.repository.Query("select e.problem.id as problemId, e.published as published from Editorial e where e.problem.id in :ids")
java.util.List<EditorialStatus> statuses(@org.springframework.data.repository.query.Param("ids") java.util.List<Long> ids);
interface EditorialStatus {Long getProblemId(); boolean isPublished();}
}
