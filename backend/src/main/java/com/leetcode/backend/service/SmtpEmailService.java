package com.leetcode.backend.service;

import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** SMTP/Brevo-compatible delivery only. Visual rendering belongs to EmailTemplateService. */
@Service
@Primary
@ConditionalOnProperty(prefix = "verdixa.mail", name = "enabled", havingValue = "true")
public class SmtpEmailService implements EmailService {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy 'at' h:mm a z");
    private static final String SECURITY_WARNING = "Never share this code with anyone. Verdixa will never ask you to disclose your OTP.";

    private final JavaMailSender sender;
    private final String from, fromName, host, username, password, frontendBaseUrl;
    private final boolean smtpAuthentication;
    private final ZoneId appZone;
    private final byte[] logo;

    public SmtpEmailService(JavaMailSender sender,
                            @Value("${verdixa.mail.from:}") String from,
                            @Value("${verdixa.mail.from-name:Verdixa}") String fromName,
                            @Value("${spring.mail.host:}") String host,
                            @Value("${spring.mail.username:}") String username,
                            @Value("${spring.mail.password:}") String password,
                            @Value("${spring.mail.properties.mail.smtp.auth:true}") boolean smtpAuthentication,
                            @Value("${verdixa.frontend-base-url}") String frontendBaseUrl,
                            @Value("${verdixa.app-time-zone:UTC}") String appTimeZone) {
        this.sender = sender; this.from = from; this.fromName = fromName; this.host = host; this.username = username;
        this.password = password; this.smtpAuthentication = smtpAuthentication; this.frontendBaseUrl = frontendBaseUrl.replaceAll("/+$", "");
        this.appZone = ZoneId.of(appTimeZone); this.logo = loadLogo();
    }

    @PostConstruct
    void validateConfiguration() {
        if (host.isBlank() || from.isBlank() || (smtpAuthentication && (username.isBlank() || password.isBlank())))
            throw new IllegalStateException("MAIL_HOST, MAIL_FROM, and SMTP credentials are required when authenticated mail is enabled.");
        try { new InternetAddress(from, true).validate(); }
        catch (Exception exception) { throw new IllegalStateException("MAIL_FROM must be a valid email address.", exception); }
    }

    @Override public void sendVerificationCode(String recipient, String username, String otp) {
        deliver(recipient, "Your Verdixa verification code", EmailTemplateService.buildBoldGradientOtpEmail(username,
                "You’re Almost There!", "Verify your email to unlock your Verdixa account and continue.", otp, "10 minutes",
                "Verify Email", frontendBaseUrl + "/verify-email", SECURITY_WARNING));
    }
    @Override public void sendPasswordResetCode(String recipient, String username, String otp) {
        deliver(recipient, "Your Verdixa password reset code", EmailTemplateService.buildMinimalLuxuryEmail(username,
                "Reset Your Password", "Use the code below to reset the password for your Verdixa account.", null, null, otp,
                "This code expires in 10 minutes. If you didn't request this, you can safely ignore this email."));
    }
    @Override public void sendWelcome(String recipient, String username) {
        deliver(recipient, "Welcome to Verdixa", EmailTemplateService.buildMinimalLuxuryEmail(username, "Welcome to Verdixa",
                "Your email is verified and your workspace is ready.", "Sign in to Verdixa", frontendBaseUrl + "/login", null,
                "Build momentum by solving problems, joining contests, and tracking your progress."));
    }
    @Override public void sendUsernameChangeCode(String recipient, String username, String requestedUsername, String otp) {
        securityCode(recipient, username, "Authorize your username change", "This code authorizes changing your Verdixa username to " + requestedUsername + ".", otp);
    }
    @Override public void sendUsernameChanged(String recipient, String previousUsername, String newUsername) {
        securityNotice(recipient, "Your Verdixa username was changed", newUsername, "The username for this account changed from " + previousUsername + " to " + newUsername + ". If this was not you, secure your email account and contact support.");
    }
    @Override public void sendEmailChangeCurrentCode(String recipient, String username, String requestedEmail, String otp) {
        securityCode(recipient, username, "Confirm your email change", "This code confirms that you authorized changing your Verdixa email to " + requestedEmail + ".", otp);
    }
    @Override public void sendEmailChangeNewCode(String recipient, String username, String otp) {
        securityCode(recipient, username, "Verify your new email", "Use this code to prove ownership of this email address before Verdixa updates your account.", otp);
    }
    @Override public void sendEmailChangedNotice(String recipient, String username, String newEmail) {
        securityNotice(recipient, "Your Verdixa email was changed", username, "The registered email for this account was changed to " + newEmail + ". If this was not you, secure your account immediately.");
    }
    @Override public void sendPasswordSetupCode(String recipient, String username, String otp) {
        securityCode(recipient, username, "Set a Verdixa password", "This code authorizes adding a local password while keeping your connected sign-in methods active.", otp);
    }
    @Override public void sendPasswordChangedNotice(String recipient, String username) {
        securityNotice(recipient, "Your Verdixa password was updated", username, "The local password for this account was updated. If this was not you, secure your email account and reset your password immediately.");
    }

    @Override public void sendContestRegistration(String recipient, String username, String contestTitle, String description, LocalDateTime startAt, LocalDateTime endAt, int problemCount, Long contestId) {
        deliver(recipient, "Registered: " + contestTitle + " | Verdixa", dashboard(username, "Your Contest is Ready",
                blank(description) ? "Your contest registration is confirmed." : description, "Contest", contestTitle, startAt, endAt,
                "Registered", "View Contest", frontendBaseUrl + "/contests/" + contestId));
    }
    @Override public void sendContestNotice(String recipient, String username, String title, LocalDateTime startAt, LocalDateTime endAt, Long contestId, String type) {
        boolean live = "CONTEST_LIVE".equals(type);
        deliver(recipient, (live ? "Your Contest is Live" : "Your Contest is Ready") + ": " + title, dashboard(username,
                live ? "Your Contest is Live" : "Your Contest is Ready", live ? "Your Verdixa contest has started. Good luck." : "Your Verdixa contest schedule is below.",
                "Contest", title, startAt, endAt, live ? "Live" : "Scheduled", live ? "Start Contest" : "View Contest", frontendBaseUrl + "/contests/" + contestId));
    }
    @Override public void sendCreatorOtp(String recipient, String username, String otp) {
        securityCode(recipient, username, "Confirm your creator application", "Use this code to submit your Assessment Creator application for admin review.", otp);
    }
    @Override public void sendCreatorDecision(String recipient, String username, boolean approved, String reason) {
        String detail = approved ? "Your Assessment Creator verification has been approved." : "Your Assessment Creator application was not approved. You may revise it and submit a new application." + (blank(reason) ? "" : " Review note: " + reason);
        deliver(recipient, (approved ? "Assessment Creator access approved" : "Assessment Creator application update") + " | Verdixa",
                EmailTemplateService.buildMinimalLuxuryEmail(username, approved ? "Creator Access Approved" : "Application Update", detail,
                        approved ? "Open Assessment Studio" : "Revise Application", frontendBaseUrl + (approved ? "/assessments/studio" : "/assessment-creator/apply"), null, "This is an account notification from Verdixa."));
    }
    @Override public void sendAssessmentInvitation(String recipient, String username, String title, String host, String organization, LocalDateTime startAt, LocalDateTime endAt, Long assessmentId) {
        enterprise(recipient, username, "Assessment Invitation", host + " invited you to take an assessment on Verdixa for " + organization + ".", title, startAt, endAt, "Online", "Invitation Pending",
                List.of("Do not share access credentials.", "Contact the assessment organizer if you need assistance."), "View Assessment", frontendBaseUrl + "/assessments/" + assessmentId);
    }
    @Override public void sendAssessmentNotice(String recipient, String username, String title, String host, String organization, String description, LocalDateTime startAt, LocalDateTime endAt, LocalDateTime registrationDeadline, int problemCount, boolean fullscreenRequired, boolean microphoneRequired, Long assessmentId, String type, String accessToken) {
        String accessUrl = frontendBaseUrl + "/assessment/" + assessmentId + "/access" + (blank(accessToken) ? "" : "?invite=" + java.net.URLEncoder.encode(accessToken, StandardCharsets.UTF_8));
        if ("ASSESSMENT_REGISTRATION_CONFIRMATION".equals(type)) {
            deliver(recipient, "Assessment scheduled: " + title + " | Verdixa", dashboard(username, "Your Assessment is Ready", blank(description) ? "Sharpen your skills. Take the next step." : description,
                    "Assessment", title, startAt, endAt, "Scheduled", "Go to Assessment", accessUrl)); return;
        }
        if ("ASSESSMENT_CANCELLED".equals(type)) {
            deliver(recipient, "Assessment update: " + title + " | Verdixa", EmailTemplateService.buildMinimalLuxuryEmail(username, "Assessment Cancelled", title + " has been cancelled. Please contact the assessment organizer if you need assistance.", null, null, null, "This is a scheduling notification from Verdixa.")); return;
        }
        boolean live = "ASSESSMENT_LIVE".equals(type);
        enterprise(recipient, username, live ? "Your Assessment is Live" : (type.startsWith("ASSESSMENT_REMINDER_") ? "Assessment Reminder" : "Assessment Invitation"),
                live ? "Your Verdixa assessment is now live. Use the details below to begin." : (blank(description) ? host + " shared an assessment with you on Verdixa." : description), title, startAt, endAt,
                mode(fullscreenRequired, microphoneRequired), live ? "Live" : "Scheduled", assessmentInstructions(fullscreenRequired, microphoneRequired, registrationDeadline), live ? "Start Assessment" : "Access Assessment", accessUrl);
    }
    @Override public void sendAssessmentAccessOtp(String recipient, String name, String assessmentTitle, String otp) {
        deliver(recipient, "Your Verdixa assessment verification code", EmailTemplateService.buildBoldGradientOtpEmail(name, "You’re Almost There!", "Verify your email to continue to " + assessmentTitle + ".", otp, "10 minutes", null, null, SECURITY_WARNING));
    }
    @Override public void sendAssessmentAccessInvitation(String recipient, String name, String title, String host, String organization, LocalDateTime startAt, LocalDateTime endAt, int problemCount, boolean fullscreen, boolean microphone, String accessUrl) {
        enterprise(recipient, name, "Assessment Invitation", host + " invited you to complete an assessment for " + organization + ".", title, startAt, endAt,
                mode(fullscreen, microphone), "Invitation Pending", assessmentInstructions(fullscreen, microphone, null), "Access Assessment", accessUrl);
    }

    private void enterprise(String recipient, String name, String heading, String intro, String title, LocalDateTime startAt, LocalDateTime endAt, String mode, String status, List<String> instructions, String cta, String url) {
        deliver(recipient, heading + ": " + title + " | Verdixa", EmailTemplateService.buildEnterpriseAssessmentEmail(name, heading, intro,
                new EmailTemplateService.AssessmentDetails(title, format(startAt), duration(startAt, endAt), "", mode, status, instructions), cta, url));
    }
    private EmailTemplateService.RenderedEmail dashboard(String name, String heading, String intro, String itemLabel, String itemName, LocalDateTime start, LocalDateTime end, String status, String cta, String url) {
        return EmailTemplateService.buildDashboardEmail(name, heading, intro, new EmailTemplateService.DashboardDetails(itemLabel, itemName, format(start), duration(start, end), status, "Opportunities don’t happen. You create them."), cta, url);
    }
    private void securityCode(String recipient, String username, String heading, String explanation, String otp) {
        deliver(recipient, heading + " | Verdixa", EmailTemplateService.buildBoldGradientOtpEmail(username, heading, explanation, otp, "10 minutes", null, null, SECURITY_WARNING));
    }
    private void securityNotice(String recipient, String subject, String username, String explanation) {
        deliver(recipient, subject, EmailTemplateService.buildMinimalLuxuryEmail(username, "Security Notice", explanation, null, null, null, "Review this message if you did not make this change."));
    }
    private void deliver(String recipient, String subject, EmailTemplateService.RenderedEmail email) {
        try {
            MimeMessage message = sender.createMimeMessage(); MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(new InternetAddress(from, fromName, StandardCharsets.UTF_8.name())); helper.setTo(recipient); helper.setSubject(subject); helper.setText(email.text(), email.html());
            helper.addInline(EmailTemplateService.LOGO_CONTENT_ID, new ByteArrayResource(logo), "image/png"); sender.send(message);
        } catch (Exception exception) { throw new IllegalStateException("Email delivery failed.", exception); }
    }
    private String format(LocalDateTime dateTime) { return dateTime.atZone(appZone).format(DATE_TIME); }
    private static String duration(LocalDateTime startAt, LocalDateTime endAt) { return Math.max(0, Duration.between(startAt, endAt).toMinutes()) + " minutes"; }
    private static String mode(boolean fullscreen, boolean microphone) { return fullscreen && microphone ? "Online (fullscreen + microphone)" : fullscreen ? "Online (fullscreen)" : microphone ? "Online (microphone)" : "Online"; }
    private static List<String> assessmentInstructions(boolean fullscreen, boolean microphone, LocalDateTime deadline) {
        List<String> instructions = new ArrayList<>(); if (fullscreen) instructions.add("Full-screen mode is required for this assessment."); if (microphone) instructions.add("A working microphone is required for this assessment.");
        if (deadline != null) instructions.add("Complete access before the registration deadline: " + deadline + "."); instructions.add("Do not share access credentials or verification codes."); return instructions;
    }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static byte[] loadLogo() { try (var stream = new ClassPathResource("email/verdixa-original-logo.png").getInputStream()) { return stream.readAllBytes(); } catch (IOException exception) { throw new IllegalStateException("The Verdixa email logo asset is missing.", exception); } }
}
