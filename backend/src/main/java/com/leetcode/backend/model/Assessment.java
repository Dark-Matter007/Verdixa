package com.leetcode.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="assessments", indexes={@Index(name="idx_assessment_creator",columnList="creator_id,status"),@Index(name="idx_assessment_visibility_schedule",columnList="visibility,start_at,end_at")})
public class Assessment {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="creator_id",nullable=false) private User creator;
 @Column(nullable=false,length=180) private String title;
 @Column(nullable=false,length=180) private String organization;
 @Column(columnDefinition="TEXT") private String description;
 @Column(columnDefinition="TEXT") private String instructions;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private AssessmentEnums.Visibility visibility;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private AssessmentEnums.Status status=AssessmentEnums.Status.DRAFT;
 @Column(name="start_at",nullable=false) private LocalDateTime startAt;
 @Column(name="end_at",nullable=false) private LocalDateTime endAt;
 @Column(name="registration_deadline") private LocalDateTime registrationDeadline;
 @Column(name="max_participants") private Integer maxParticipants;
 @Column(name="fullscreen_required",nullable=false) private boolean fullscreenRequired=true;
 @Column(name="microphone_required",nullable=false) private boolean microphoneRequired=true;
 @Column(name="strict_proctoring",nullable=false) private boolean strictProctoring=true;
 @Column(name="invite_token",nullable=false,unique=true,length=64) private String inviteToken;
 @Column(name="mcq_questions",columnDefinition="TEXT") private String mcqQuestions;
 @Column(name="archived_at") private LocalDateTime archivedAt;
 @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
 @Column(name="updated_at",nullable=false) private LocalDateTime updatedAt;
 @PrePersist void create(){createdAt=updatedAt=LocalDateTime.now();}
 @PreUpdate void update(){updatedAt=LocalDateTime.now();}
 public Long getId(){return id;} public User getCreator(){return creator;} public void setCreator(User v){creator=v;}
 public String getTitle(){return title;} public void setTitle(String v){title=v;} public String getOrganization(){return organization;} public void setOrganization(String v){organization=v;}
 public String getDescription(){return description;} public void setDescription(String v){description=v;} public String getInstructions(){return instructions;} public void setInstructions(String v){instructions=v;}
 public AssessmentEnums.Visibility getVisibility(){return visibility;} public void setVisibility(AssessmentEnums.Visibility v){visibility=v;} public AssessmentEnums.Status getStatus(){return status;} public void setStatus(AssessmentEnums.Status v){status=v;}
 public LocalDateTime getStartAt(){return startAt;} public void setStartAt(LocalDateTime v){startAt=v;} public LocalDateTime getEndAt(){return endAt;} public void setEndAt(LocalDateTime v){endAt=v;}
 public LocalDateTime getRegistrationDeadline(){return registrationDeadline;} public void setRegistrationDeadline(LocalDateTime v){registrationDeadline=v;} public Integer getMaxParticipants(){return maxParticipants;} public void setMaxParticipants(Integer v){maxParticipants=v;}
 public boolean isFullscreenRequired(){return fullscreenRequired;} public void setFullscreenRequired(boolean v){fullscreenRequired=v;} public boolean isMicrophoneRequired(){return microphoneRequired;} public void setMicrophoneRequired(boolean v){microphoneRequired=v;} public boolean isStrictProctoring(){return strictProctoring;} public void setStrictProctoring(boolean v){strictProctoring=v;}
 public String getInviteToken(){return inviteToken;} public void setInviteToken(String v){inviteToken=v;} public String getMcqQuestions(){return mcqQuestions;} public void setMcqQuestions(String v){mcqQuestions=v;} public LocalDateTime getArchivedAt(){return archivedAt;} public void setArchivedAt(LocalDateTime v){archivedAt=v;}
 public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getUpdatedAt(){return updatedAt;}
 public AssessmentEnums.Status derivedStatus(){if(status==AssessmentEnums.Status.DRAFT||status==AssessmentEnums.Status.CANCELLED)return status;var now=LocalDateTime.now();return now.isBefore(startAt)?AssessmentEnums.Status.UPCOMING:now.isBefore(endAt)?AssessmentEnums.Status.LIVE:AssessmentEnums.Status.COMPLETED;}
}
