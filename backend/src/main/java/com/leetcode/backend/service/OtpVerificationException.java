package com.leetcode.backend.service;

public class OtpVerificationException extends RuntimeException {
    public OtpVerificationException() { super("Invalid or expired verification code."); }
}
