package com.leetcode.backend.repository;
import com.leetcode.backend.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface UserCertificateRepository extends JpaRepository<UserCertificate,Long> {
    List<UserCertificate> findByUserIdOrderByMilestoneAsc(Long userId);
    Optional<UserCertificate> findByPublicCertificateId(String publicCertificateId);
    @org.springframework.data.jpa.repository.Query("select c.user.id as userId, c.milestone as milestone from UserCertificate c where c.user.id in :ids")
    List<UserAward> awardsByUsers(@org.springframework.data.repository.query.Param("ids") List<Long> ids);
    interface UserAward { Long getUserId(); CertificateMilestone getMilestone(); }
    long countByMilestone(CertificateMilestone milestone);
}
