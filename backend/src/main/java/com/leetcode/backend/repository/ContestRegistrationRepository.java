package com.leetcode.backend.repository;

import com.leetcode.backend.model.ContestRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface ContestRegistrationRepository extends JpaRepository<ContestRegistration,Long>{
 Optional<ContestRegistration> findByContestIdAndUserId(Long contestId,Long userId);
 List<ContestRegistration> findByContestId(Long contestId);
 @Query("select r from ContestRegistration r join fetch r.contest where r.user.id = :userId order by r.contest.startAt asc")
 List<ContestRegistration> findWithContestByUserId(@Param("userId") Long userId);
 @Query("select r from ContestRegistration r join fetch r.user where r.contest.id = :contestId order by r.registeredAt asc")
 List<ContestRegistration> findWithUserByContestIdOrderByRegisteredAtAsc(@Param("contestId") Long contestId);
 long countByContestId(Long contestId);
 @Modifying
 @Query("delete from ContestRegistration r where r.contest.id = :contestId")
 int deleteAllByContestId(@Param("contestId") Long contestId);
}
