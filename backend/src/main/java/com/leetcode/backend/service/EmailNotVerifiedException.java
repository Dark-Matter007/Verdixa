package com.leetcode.backend.service;

public class EmailNotVerifiedException extends RuntimeException {
    public EmailNotVerifiedException() { super("Verify your email address before signing in."); }
}
