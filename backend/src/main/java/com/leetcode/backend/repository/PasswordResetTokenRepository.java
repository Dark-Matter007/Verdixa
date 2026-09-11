package com.leetcode.backend.repository;
import com.leetcode.backend.model.PasswordResetToken;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from PasswordResetToken token where token.user.id = :userId")
    Optional<PasswordResetToken> findByUserIdForUpdate(@Param("userId") Long userId);
}
