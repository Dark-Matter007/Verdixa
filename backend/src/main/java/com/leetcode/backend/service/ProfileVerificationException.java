package com.leetcode.backend.service;

/** Kept separate so a failed OTP attempt can be committed while returning a friendly 400. */
public class ProfileVerificationException extends RuntimeException {
    public ProfileVerificationException(String message) { super(message); }
}
