package com.leetcode.backend.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "editorial_reveals", uniqueConstraints = @UniqueConstraint(name = "uk_editorial_reveal", columnNames = {"user_id", "problem_id"}))
public class EditorialReveal {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private User user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "problem_id") private Problem problem;
    @Column(nullable = false, updatable = false) private Instant revealedAt;
    protected EditorialReveal() {}
    public EditorialReveal(User user, Problem problem) { this.user = user; this.problem = problem; this.revealedAt = Instant.now(); }
    public Long getId() { return id; }
    public Instant getRevealedAt() { return revealedAt; }
}
