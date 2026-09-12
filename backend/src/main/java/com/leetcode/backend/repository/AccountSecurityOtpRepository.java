package com.leetcode.backend.repository;

import com.leetcode.backend.model.AccountOtpPurpose;
import com.leetcode.backend.model.AccountSecurityOtp;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface AccountSecurityOtpRepository extends JpaRepository<AccountSecurityOtp, Long> {
    Optional<AccountSecurityOtp> findByUserIdAndPurpose(Long userId, AccountOtpPurpose purpose);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select otp from AccountSecurityOtp otp where otp.user.id = :userId and otp.purpose = :purpose")
    Optional<AccountSecurityOtp> findForUpdate(@Param("userId") Long userId, @Param("purpose") AccountOtpPurpose purpose);
}
