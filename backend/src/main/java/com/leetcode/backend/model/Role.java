package com.leetcode.backend.model;

public enum Role {
    USER,
    ADMIN,
    /**
     * A verified assessment-only identity. It deliberately cannot use the
     * normal learner application APIs even if a password-reset link is used.
     */
    ASSESSMENT_GUEST
}
