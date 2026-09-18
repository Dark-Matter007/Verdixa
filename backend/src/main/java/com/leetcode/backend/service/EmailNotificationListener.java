package com.leetcode.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.scheduling.annotation.Async;

/** Delivery happens only after a successful database commit and can never roll it back. */
@Component
public class EmailNotificationListener {
    private static final Logger log = LoggerFactory.getLogger(EmailNotificationListener.class);
    private final EmailService emailService;
    private final NotificationDeliveryService deliveries;

    public EmailNotificationListener(EmailService emailService, NotificationDeliveryService deliveries) { this.emailService = emailService; this.deliveries = deliveries; }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void verificationIssued(EmailNotificationEvents.OtpIssued event) {
        attempt(() -> emailService.sendVerificationCode(event.email(), event.username(), event.otp()), "verification");
    }
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void passwordResetIssued(EmailNotificationEvents.PasswordResetOtpIssued event) {
        attempt(() -> emailService.sendPasswordResetCode(event.email(), event.username(), event.otp()), "password reset");
    }
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void accountVerified(EmailNotificationEvents.AccountVerified event) {
        attempt(() -> emailService.sendWelcome(event.email(), event.username()), "welcome");
    }
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void usernameChangeOtp(EmailNotificationEvents.UsernameChangeOtpIssued event) {
        attempt(() -> emailService.sendUsernameChangeCode(event.email(), event.username(), event.requestedUsername(), event.otp()), "username change code");
    }
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void usernameChanged(EmailNotificationEvents.UsernameChanged event) {
        attempt(() -> emailService.sendUsernameChanged(event.email(), event.previousUsername(), event.newUsername()), "username changed notice");
    }
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void emailChangeCurrentOtp(EmailNotificationEvents.EmailChangeCurrentOtpIssued event) {
        attempt(() -> emailService.sendEmailChangeCurrentCode(event.email(), event.username(), event.requestedEmail(), event.otp()), "current email confirmation");
    }
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void emailChangeNewOtp(EmailNotificationEvents.EmailChangeNewOtpIssued event) {
        attempt(() -> emailService.sendEmailChangeNewCode(event.email(), event.username(), event.otp()), "new email verification");
    }
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void emailChanged(EmailNotificationEvents.EmailChanged event) {
        attempt(() -> emailService.sendEmailChangedNotice(event.previousEmail(), event.username(), event.newEmail()), "email changed notice");
    }
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void passwordSetupOtp(EmailNotificationEvents.PasswordSetupOtpIssued event) {
        attempt(() -> emailService.sendPasswordSetupCode(event.email(), event.username(), event.otp()), "password setup code");
    }
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void passwordChanged(EmailNotificationEvents.PasswordChanged event) {
        attempt(() -> emailService.sendPasswordChangedNotice(event.email(), event.username()), "password changed notice");
    }
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void contestRegistered(EmailNotificationEvents.ContestRegistered event) {
        attempt(() -> emailService.sendContestRegistration(event.email(), event.username(), event.title(), event.description(), event.startAt(), event.endAt(), event.problemCount(), event.contestId()), "contest registration");
    }
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void contestCreated(EmailNotificationEvents.ContestCreated event) { deliveries.queueContestAnnouncement(event.contestId()); }
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void creatorOtp(EmailNotificationEvents.CreatorOtpIssued event) { attempt(() -> emailService.sendCreatorOtp(event.email(),event.username(),event.otp()), "creator verification OTP"); }
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void creatorDecision(EmailNotificationEvents.CreatorDecision event) { attempt(() -> emailService.sendCreatorDecision(event.email(),event.username(),event.approved(),event.reason()), "creator decision"); }
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void assessmentPublished(EmailNotificationEvents.AssessmentPublished event) { deliveries.queueAssessmentPublished(event.assessmentId()); }
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void assessmentInvitationsChanged(EmailNotificationEvents.AssessmentInvitationsChanged event) { deliveries.queueAssessmentInvitations(event.assessmentId()); }
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void assessmentRegistered(EmailNotificationEvents.AssessmentRegistered event) { deliveries.queueAssessmentRegistration(event.assessmentId(), event.userId()); }
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void assessmentUpdated(EmailNotificationEvents.AssessmentUpdated event) { deliveries.queueAssessmentUpdate(event.assessmentId()); }
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void assessmentCancelled(EmailNotificationEvents.AssessmentCancelled event) { deliveries.queueAssessmentCancellation(event.assessmentId()); }
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void assessmentAccessInvitation(EmailNotificationEvents.AssessmentAccessInvitation event) { attempt(() -> emailService.sendAssessmentAccessInvitation(event.email(),event.name(),event.title(),event.host(),event.organization(),event.startAt(),event.endAt(),event.problemCount(),event.fullscreen(),event.microphone(),event.accessUrl()), "assessment invitation"); }
    private void attempt(Runnable send, String type) {
        try {
            send.run();
        } catch (RuntimeException exception) {
            Throwable root = exception;
            while (root.getCause() != null) root = root.getCause();
            log.warn("{} email delivery failed after commit: {}: {}", type,
                    root.getClass().getSimpleName(), safeMessage(root));
        }
    }

    /** Provider errors help operators fix configuration without logging OTPs or credentials. */
    private static String safeMessage(Throwable exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) return "No provider error message was supplied.";
        return message.replaceAll("[\\r\\n]+", " ");
    }
}
