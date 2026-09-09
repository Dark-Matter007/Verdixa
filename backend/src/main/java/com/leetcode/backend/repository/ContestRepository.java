package com.leetcode.backend.repository;

import com.leetcode.backend.model.Contest;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.*;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.Query;

public interface ContestRepository extends JpaRepository<Contest,Long>{
 Optional<Contest> findBySlug(String slug);
 boolean existsBySlug(String slug);
 @Query("""
         select c.id as id, c.title as title, c.startAt as startTime, c.endAt as endTime,
                count(distinct cp.id) as problemCount, count(distinct r.id) as registrationCount
         from Contest c
         left join ContestProblem cp on cp.contest.id = c.id
         left join ContestRegistration r on r.contest.id = c.id
         group by c.id, c.title, c.startAt, c.endAt
         order by c.startAt desc
         """)
 List<AdminContestRow> findAdminRows();
 interface AdminContestRow {
  Long getId(); String getTitle(); LocalDateTime getStartTime(); LocalDateTime getEndTime();
  long getProblemCount(); long getRegistrationCount();
 }
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 Optional<Contest> findWithProblemSetLockById(Long id);
}
