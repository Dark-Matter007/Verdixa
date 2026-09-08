package com.leetcode.backend.model;
public enum CertificateMilestone {
    PROBLEMS_50(50), PROBLEMS_100(100), PROBLEMS_150(150);
    private final int target;
    CertificateMilestone(int target) { this.target = target; }
    public int getTarget() { return target; }
}
