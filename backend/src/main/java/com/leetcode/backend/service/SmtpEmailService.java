package com.leetcode.backend.service;

import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Primary;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
@Primary
@ConditionalOnProperty(prefix = "verdixa.mail", name = "enabled", havingValue = "true")
public class SmtpEmailService implements EmailService {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy 'at' h:mm a z");

    private final JavaMailSender sender;
    private final String from;
    private final String fromName;
    private final String host;
    private final String username;
    private final String password;
    private final boolean smtpAuthentication;
    private final String frontendBaseUrl;
    private final ZoneId appZone;

    public SmtpEmailService(JavaMailSender sender,
                            @Value("${verdixa.mail.from:}") String from,
                            @Value("${verdixa.mail.from-name:Verdixa}") String fromName,
                            @Value("${spring.mail.host:}") String host,
                            @Value("${spring.mail.username:}") String username,
                            @Value("${spring.mail.password:}") String password,
                            @Value("${spring.mail.properties.mail.smtp.auth:true}") boolean smtpAuthentication,
                            @Value("${verdixa.frontend-base-url}") String frontendBaseUrl,
                            @Value("${verdixa.app-time-zone:UTC}") String appTimeZone) {
        this.sender = sender;
        this.from = from;
        this.fromName = fromName;
        this.host = host;
        this.username = username;
        this.password = password;
        this.smtpAuthentication = smtpAuthentication;
        this.frontendBaseUrl = frontendBaseUrl.replaceAll("/+$", "");
        this.appZone = ZoneId.of(appTimeZone);
    }

    @PostConstruct
    void validateConfiguration() {
        if (host.isBlank() || from.isBlank() || (smtpAuthentication && (username.isBlank() || password.isBlank()))) {
            throw new IllegalStateException("MAIL_HOST, MAIL_FROM, and SMTP credentials are required when authenticated mail is enabled.");
        }
        try {
            new InternetAddress(from, true).validate();
        } catch (Exception exception) {
            throw new IllegalStateException("MAIL_FROM must be a valid email address.", exception);
        }
    }

    @Override
    public void sendVerificationCode(String recipient, String username, String otp) {
        String safeName = escape(username);
        send(recipient, "Your Verdixa verification code",
                "Hello " + username + ",\n\nYour Verdixa verification code is " + otp
                        + ". It expires in 10 minutes. If you did not create this account, you can ignore this email.",
                layout("Verify your email", "Hello " + safeName + ",", "Use this code to verify your Verdixa account. It expires in 10 minutes.",
                        "<div class=\"code\">" + otp + "</div><p class=\"muted\">If you did not create this account, you can ignore this email.</p>"));
    }

    @Override
    public void sendWelcome(String recipient, String username) {
        String safeName = escape(username);
        String login = frontendBaseUrl + "/login";
        send(recipient, "Welcome to Verdixa",
                "Hello " + username + ",\n\nYour email is verified and your Verdixa account is ready. Sign in at " + login
                        + " to solve problems, join contests, track submissions, and view your progress.",
                layout("Welcome to Verdixa", "Hello " + safeName + ",", "Your email is verified and your workspace is ready.",
                        "<p>Build momentum by solving problems, joining contests, tracking submissions, and reviewing your progress.</p>"
                                + button(login, "Sign in to Verdixa")));
    }

    @Override
    public void sendContestRegistration(String recipient, String username, String contestTitle,
                                        String description, LocalDateTime startAt, LocalDateTime endAt,
                                        int problemCount, Long contestId) {
        String link = frontendBaseUrl + "/contests/" + contestId;
        String start = format(startAt), end = format(endAt);
        String descriptionText = description == null || description.isBlank() ? "" : "\n\n" + description;
        send(recipient, "Registered: " + contestTitle + " | Verdixa",
                "Hello " + username + ",\n\nYou are registered for " + contestTitle + "." + descriptionText
                        + "\n\nStarts: " + start + "\nEnds: " + end + "\nProblems: " + problemCount
                        + "\n\nLog in before the contest starts: " + link,
                layout("Contest registration confirmed", "Hello " + escape(username) + ",",
                        "You are registered for <strong>" + escape(contestTitle) + "</strong>.",
                        (description == null || description.isBlank() ? "" : "<p>" + escape(description) + "</p>")
                                + "<table><tr><td>Starts</td><td>" + escape(start) + "</td></tr><tr><td>Ends</td><td>" + escape(end)
                                + "</td></tr><tr><td>Problems</td><td>" + problemCount + "</td></tr></table>"
                                + "<p>Log in before the contest starts so you are ready when the arena opens.</p>" + button(link, "View contest")));
    }

    private void send(String recipient, String subject, String plainText, String html) {
        try {
            MimeMessage message = sender.createMimeMessage();
            // A verification email deliberately includes a plain-text fallback and an HTML body.
            // Spring requires multipart mode when both alternatives are present.
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(new InternetAddress(from, fromName, StandardCharsets.UTF_8.name()));
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(plainText, html);
            sender.send(message);
        } catch (Exception exception) {
            throw new IllegalStateException("Email delivery failed.", exception);
        }
    }

    private String format(LocalDateTime dateTime) { return dateTime.atZone(appZone).format(DATE_TIME); }
    private static String button(String href, String label) { return "<p><a class=\"button\" href=\"" + escapeAttribute(href) + "\">" + escape(label) + "</a></p>"; }
    private static String layout(String heading, String greeting, String intro, String content) {
        return "<!doctype html><html><body style=\"margin:0;background:#f6f3ec;font-family:Arial,sans-serif;color:#1b1b1b\"><main style=\"max-width:600px;margin:24px auto;background:#fff;padding:32px;border-top:4px solid #b88916\"><p style=\"font-size:12px;letter-spacing:2px;color:#876810\">VERDIXA</p><h1>" + heading + "</h1><p>" + greeting + "</p><p>" + intro + "</p>" + content + "<hr style=\"border:0;border-top:1px solid #e5dfd4;margin-top:28px\"><p class=\"muted\" style=\"color:#6b655b;font-size:13px\">Verdixa · Practice with purpose</p></main><style>.code{font-size:28px;font-weight:bold;letter-spacing:8px;padding:16px;background:#f6f3ec;text-align:center}.button{display:inline-block;padding:12px 18px;background:#1b1b1b;color:#fff!important;text-decoration:none;border-radius:4px}table{width:100%;border-collapse:collapse}td{padding:9px 0;border-bottom:1px solid #e5dfd4}td:first-child{color:#6b655b;width:30%}</style></body></html>";
    }
    private static String escape(String value) { return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;"); }
    private static String escapeAttribute(String value) { return escape(value); }
}
