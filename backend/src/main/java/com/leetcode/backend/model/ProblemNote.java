package com.leetcode.backend.model;
import jakarta.persistence.*; import java.time.LocalDateTime;
@Entity @Table(name="problem_notes",uniqueConstraints=@UniqueConstraint(columnNames={"user_id","problem_id"})) public class ProblemNote {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @ManyToOne(optional=false) @JoinColumn(name="user_id") private User user; @ManyToOne(optional=false) @JoinColumn(name="problem_id") private Problem problem; @Column(columnDefinition="TEXT",nullable=false) private String content; private LocalDateTime updatedAt;
 @PrePersist @PreUpdate void stamp(){updatedAt=LocalDateTime.now();} public Long getId(){return id;} public User getUser(){return user;} public void setUser(User v){user=v;} public Problem getProblem(){return problem;} public void setProblem(Problem v){problem=v;} public String getContent(){return content;} public void setContent(String v){content=v;} public LocalDateTime getUpdatedAt(){return updatedAt;}
}
