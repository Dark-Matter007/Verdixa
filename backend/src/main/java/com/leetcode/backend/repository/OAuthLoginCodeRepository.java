package com.leetcode.backend.repository;

import com.leetcode.backend.model.OAuthLoginCode;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface OAuthLoginCodeRepository extends JpaRepository<OAuthLoginCode, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from OAuthLoginCode c join fetch c.user where c.codeHash = :hash")
    Optional<OAuthLoginCode> findByCodeHashForUpdate(@Param("hash") String hash);
}
