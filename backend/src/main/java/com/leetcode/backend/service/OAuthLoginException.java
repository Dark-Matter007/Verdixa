package com.leetcode.backend.service;

public class OAuthLoginException extends RuntimeException {
    private final String reason;
    public OAuthLoginException(String reason) { this.reason = reason; }
    public String getReason() { return reason; }
}
