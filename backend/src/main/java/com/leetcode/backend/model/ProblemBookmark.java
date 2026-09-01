package com.leetcode.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "problem_bookmarks", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "problem_id"}), indexes = {
        @Index(name = "idx_bookmark_user", columnList = "user_id"),
        @Index(name = "idx_bookmark_problem", columnList = "problem_id")
})
public class ProblemBookmark {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Problem getProblem() { return problem; }
    public void setProblem(Problem problem) { this.problem = problem; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
