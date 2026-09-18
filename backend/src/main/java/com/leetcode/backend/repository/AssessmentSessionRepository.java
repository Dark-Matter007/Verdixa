package com.leetcode.backend.repository;

import com.leetcode.backend.model.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.*;

public interface AssessmentSessionRepository extends JpaRepository<AssessmentSession,Long>{
    @EntityGraph(attributePaths={"assessment","participant"})
    @Query("select s from AssessmentSession s where s.id=:id")
    Optional<AssessmentSession> findAuthorizedById(@Param("id") Long id);
    Optional<AssessmentSession> findByAssessmentIdAndParticipantId(Long assessmentId,Long participantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from AssessmentSession s where s.assessment.id=:assessmentId and s.participant.id=:participantId")
    Optional<AssessmentSession> lockByAssessmentIdAndParticipantId(@Param("assessmentId") Long assessmentId,@Param("participantId") Long participantId);

    List<AssessmentSession> findByAssessmentId(Long assessmentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from AssessmentSession s where s.status=:status and s.proctoringArmedAt is not null and s.lastHeartbeatAt<:cutoff")
    List<AssessmentSession> lockArmedTimedOutSessions(@Param("status") AssessmentEnums.SessionStatus status,@Param("cutoff") LocalDateTime cutoff);

    List<AssessmentSession> findByStatusAndLastHeartbeatAtBefore(AssessmentEnums.SessionStatus status,LocalDateTime cutoff);
    long countByAssessmentIdAndStatus(Long assessmentId,AssessmentEnums.SessionStatus status);
}
