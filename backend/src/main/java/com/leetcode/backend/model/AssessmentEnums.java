package com.leetcode.backend.model;

public final class AssessmentEnums {
    private AssessmentEnums() {}
    public enum CreatorStatus { DRAFT, EMAIL_OTP_PENDING, PENDING_ADMIN_REVIEW, APPROVED, REJECTED, REVOKED }
    public enum Visibility { PUBLIC, PRIVATE }
    public enum Status { DRAFT, PUBLISHED, REGISTRATION_OPEN, UPCOMING, LIVE, COMPLETED, CANCELLED }
    public enum InvitationStatus { INVITED, REGISTERED, DECLINED, CANCELLED }
    public enum SessionStatus { ACTIVE, SUBMITTED, TERMINATED, EXPIRED }
    public enum ProctorEventType { SESSION_STARTED, FULLSCREEN_EXIT, TAB_SWITCH, MICROPHONE_DISABLED, CAMERA_DISABLED, SCREEN_SHARE_STOPPED, WINDOW_BLUR, HEARTBEAT_TIMEOUT, PAGE_EXIT, SESSION_SUBMITTED }
    public enum DeliveryStatus { PENDING, SENDING, SENT, FAILED }
}
