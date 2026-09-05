package com.leetcode.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "hint_reveals", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "hint_id"}), indexes = @Index(name = "idx_hint_reveal_user_problem", columnList = "user_id,problem_id"))
public class HintReveal {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "problem_id", nullable = false) private Problem problem;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "hint_id", nullable = false) private ProblemHint hint;
    @Column(name = "revealed_at", nullable = false) private LocalDateTime revealedAt;
    @PrePersist void created(){revealedAt=LocalDateTime.now();}
    public Long getId(){return id;} public User getUser(){return user;} public void setUser(User v){user=v;} public Problem getProblem(){return problem;} public void setProblem(Problem v){problem=v;} public ProblemHint getHint(){return hint;} public void setHint(ProblemHint v){hint=v;} public LocalDateTime getRevealedAt(){return revealedAt;}
}
