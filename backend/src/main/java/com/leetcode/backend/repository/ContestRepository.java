package com.leetcode.backend.repository;

import com.leetcode.backend.model.Contest;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.*;

public interface ContestRepository extends JpaRepository<Contest,Long>{
 Optional<Contest> findBySlug(String slug);
 boolean existsBySlug(String slug);
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 Optional<Contest> findWithProblemSetLockById(Long id);
}
