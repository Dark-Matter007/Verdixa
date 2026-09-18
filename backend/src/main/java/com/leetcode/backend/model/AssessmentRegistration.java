package com.leetcode.backend.model;
import jakarta.persistence.*; import java.time.LocalDateTime;
@Entity @Table(name="assessment_registrations",uniqueConstraints=@UniqueConstraint(columnNames={"assessment_id","user_id"}),indexes=@Index(name="idx_assessment_registration",columnList="assessment_id,user_id")) public class AssessmentRegistration {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="assessment_id",nullable=false) private Assessment assessment; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id",nullable=false) private User user; @Column(name="registered_at",nullable=false) private LocalDateTime registeredAt; @PrePersist void create(){registeredAt=LocalDateTime.now();} public Assessment getAssessment(){return assessment;} public void setAssessment(Assessment v){assessment=v;} public User getUser(){return user;} public void setUser(User v){user=v;}
}
