package com.leetcode.backend.dto;

public record ProfileActionResponse(String message, String stage, long cooldownSeconds, boolean logoutRequired) {
    public static ProfileActionResponse pending(String message, String stage, long cooldown) {
        return new ProfileActionResponse(message, stage, cooldown, false);
    }
    public static ProfileActionResponse changed(String message) {
        return new ProfileActionResponse(message, "COMPLETE", 0, true);
    }
}
