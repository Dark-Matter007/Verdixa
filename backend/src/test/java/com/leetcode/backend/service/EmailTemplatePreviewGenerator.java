package com.leetcode.backend.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Development-only command-line generator; no endpoint and no SMTP transport. */
public final class EmailTemplatePreviewGenerator {
    private EmailTemplatePreviewGenerator() { }

    public static void main(String[] args) throws IOException {
        Path output = Path.of("target", "email-previews");
        Files.createDirectories(output);
        byte[] logo;
        try (var stream = EmailTemplatePreviewGenerator.class.getClassLoader().getResourceAsStream("email/verdixa-original-logo.png")) {
            logo = stream.readAllBytes();
        }
        String url = "https://preview.verdixa.example/assessment/demo";
        write(output, "design-2-bold-gradient-otp.html", EmailTemplateService.buildBoldGradientOtpEmail("Alex", "You’re Almost There!", "Verify your email to unlock your Verdixa account and continue.", "482196", "10 minutes", "Verify Email", url, "Never share this code with anyone. Verdixa will never ask you to disclose your OTP."), logo);
        write(output, "design-3-enterprise-assessment.html", EmailTemplateService.buildEnterpriseAssessmentEmail("Alex", "Assessment Invitation", "You’ve been invited to take an assessment on Verdixa.", new EmailTemplateService.AssessmentDetails("Frontend Development Test", "Mon, 25 Nov 2026 · 10:00 AM IST", "90 minutes", "VDX-7F3K-9L2Q", "Online (fullscreen + microphone)", "Scheduled", List.of("Full-screen mode is required for this assessment.", "A working microphone is required for this assessment.", "Do not share access credentials or verification codes.")), "Start Assessment", url), logo);
        write(output, "design-4-minimal-luxury.html", EmailTemplateService.buildMinimalLuxuryEmail("Alex", "Reset Your Password", "Click the button below to create a new password for your Verdixa account.", "Reset Password", url, null, "This link expires in 1 hour."), logo);
        write(output, "design-5-product-dashboard.html", EmailTemplateService.buildDashboardEmail("Alex", "Your Assessment is Ready", "Sharpen your skills. Take the next step.", new EmailTemplateService.DashboardDetails("Assessment", "Data Structures & Algorithms", "25 Nov 2026 · 10:00 AM IST", "120 minutes", "Scheduled", "Opportunities don’t happen. You create them."), "Go to Assessment", url), logo);
    }

    private static void write(Path output, String name, EmailTemplateService.RenderedEmail email, byte[] logo) throws IOException {
        Files.writeString(output.resolve(name), EmailTemplateService.forPreview(email.html(), logo, "image/png"), StandardCharsets.UTF_8);
    }
}
