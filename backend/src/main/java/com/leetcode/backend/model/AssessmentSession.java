package com.leetcode.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "assessment_sessions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"assessment_id", "participant_id"}),
        indexes = {
                @Index(name = "idx_assessment_session", columnList = "assessment_id,status"),
                @Index(name = "idx_session_heartbeat", columnList = "status,last_heartbeat_at")
        })
public class AssessmentSession {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "assessment_id", nullable = false) private Assessment assessment;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "participant_id", nullable = false) private User participant;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private AssessmentEnums.SessionStatus status = AssessmentEnums.SessionStatus.ACTIVE;
    @Column(name = "started_at", nullable = false) private LocalDateTime startedAt;
    @Column(name = "submitted_at") private LocalDateTime submittedAt;
    @Column(name = "terminated_at") private LocalDateTime terminatedAt;
    @Column(name = "termination_reason", length = 50) private String terminationReason;
    @Column(name = "last_heartbeat_at", nullable = false) private LocalDateTime lastHeartbeatAt;
    @Column(name = "proctoring_armed_at") private LocalDateTime proctoringArmedAt;
    @Column(nullable = false) private int score;
    @Column(name = "mcq_answers", columnDefinition = "TEXT") private String mcqAnswers;

    @PrePersist
    void create() {
        LocalDateTime now = LocalDateTime.now();
        if (startedAt == null) startedAt = now;
        if (lastHeartbeatAt == null) lastHeartbeatAt = now;
    }

    public Long getId(){return id;}
    public Assessment getAssessment(){return assessment;}
    public void setAssessment(Assessment v){assessment=v;}
    public User getParticipant(){return participant;}
    public void setParticipant(User v){participant=v;}
    public AssessmentEnums.SessionStatus getStatus(){return status;}
    public void setStatus(AssessmentEnums.SessionStatus v){status=v;}
    public LocalDateTime getStartedAt(){return startedAt;}
    public void setStartedAt(LocalDateTime v){startedAt=v;}
    public LocalDateTime getSubmittedAt(){return submittedAt;}
    public void setSubmittedAt(LocalDateTime v){submittedAt=v;}
    public LocalDateTime getTerminatedAt(){return terminatedAt;}
    public void setTerminatedAt(LocalDateTime v){terminatedAt=v;}
    public String getTerminationReason(){return terminationReason;}
    public void setTerminationReason(String v){terminationReason=v;}
    public LocalDateTime getLastHeartbeatAt(){return lastHeartbeatAt;}
    public void setLastHeartbeatAt(LocalDateTime v){lastHeartbeatAt=v;}
    public LocalDateTime getProctoringArmedAt(){return proctoringArmedAt;}
    public void setProctoringArmedAt(LocalDateTime v){proctoringArmedAt=v;}
    public int getScore(){return score;}
    public void setScore(int v){score=v;}
    public String getMcqAnswers(){return mcqAnswers;}
    public void setMcqAnswers(String v){mcqAnswers=v;}
}
