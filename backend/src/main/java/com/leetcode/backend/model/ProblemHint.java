package com.leetcode.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "problem_hints", uniqueConstraints = @UniqueConstraint(columnNames = {"problem_id", "display_order"}), indexes = @Index(name = "idx_hint_problem_active_order", columnList = "problem_id,active,display_order"))
public class ProblemHint {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "problem_id", nullable = false) private Problem problem;
    @Column(nullable = false, length = 180) private String title;
    @Column(nullable = false, columnDefinition = "TEXT") private String content;
    @Column(name = "display_order", nullable = false) private int displayOrder;
    @Column(name = "penalty_points", nullable = false) private int penaltyPoints;
    @Column(nullable = false) private boolean active = true;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    @PrePersist void created() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate void updated() { updatedAt = LocalDateTime.now(); }
    public Long getId(){return id;} public Problem getProblem(){return problem;} public void setProblem(Problem v){problem=v;}
    public String getTitle(){return title;} public void setTitle(String v){title=v;} public String getContent(){return content;} public void setContent(String v){content=v;}
    public int getDisplayOrder(){return displayOrder;} public void setDisplayOrder(int v){displayOrder=v;} public int getPenaltyPoints(){return penaltyPoints;} public void setPenaltyPoints(int v){penaltyPoints=v;}
    public boolean isActive(){return active;} public void setActive(boolean v){active=v;} public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getUpdatedAt(){return updatedAt;}
}
