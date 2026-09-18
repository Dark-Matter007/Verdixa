package com.leetcode.backend.service;

import org.junit.jupiter.api.Test;

import java.time.Year;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EmailTemplateServiceTest {
    private static final String URL = "https://app.verdixa.example/assessment/42/access";

    @Test
    void boldGradientOtpUsesTheRealCodeCidLogoAndEscapesDynamicText() {
        var email = EmailTemplateService.buildBoldGradientOtpEmail("Ada <script>", "You’re Almost There!",
                "Verify <strong>your</strong> email.", "482196", "10 minutes", "Verify Email", URL,
                "Never share this code with anyone.");

        assertThat(email.html()).contains("cid:verdixa-email-logo", "482196", "Your full verification code is 482196", "@media only screen");
        assertThat(email.html()).contains("Verify &lt;strong&gt;your&lt;/strong&gt; email.");
        assertThat(email.html()).doesNotContain("localhost", "<script>");
        assertThat(email.text()).contains("482196", URL);
    }

    @Test
    void enterpriseAssessmentInsertsAllAssessmentDataAndOnlyProvidedInstructions() {
        var email = EmailTemplateService.buildEnterpriseAssessmentEmail("Alex", "Assessment Invitation", "You have been invited.",
                new EmailTemplateService.AssessmentDetails("Algorithms & <Graphs>", "25 Nov 2026 · 10:00 AM IST", "120 minutes",
                        "VDX-7F3K-9L2Q", "Online (Proctored)", "Scheduled", List.of("A working microphone is required.")),
                "Start Assessment", URL);

        assertThat(email.html()).contains("Algorithms &amp; &lt;Graphs&gt;", "25 Nov 2026 · 10:00 AM IST", "120 minutes", "VDX-7F3K-9L2Q", "Online (Proctored)", "Important Instructions");
        assertThat(email.text()).contains("Start Assessment: " + URL, "A working microphone is required.");
    }

    @Test
    void minimalLuxuryAndDashboardRemainDistinctAndUseCurrentYear() {
        var minimal = EmailTemplateService.buildMinimalLuxuryEmail("Mira", "Reset Your Password", "Create a new password.", "Reset Password", URL, null, "This link expires in 1 hour.");
        var dashboard = EmailTemplateService.buildDashboardEmail("Mira", "Your Assessment is Ready", "Sharpen your skills.",
                new EmailTemplateService.DashboardDetails("Assessment", "Data Structures", "25 Nov 2026 · 10:00 AM IST", "120 minutes", "Scheduled", "Opportunities don’t happen. You create them."),
                "Go to Assessment", URL);

        assertThat(minimal.html()).contains("Reset Your Password", "#151a20", "A more skilled tomorrow.");
        assertThat(dashboard.html()).contains("Your Assessment is Ready", "Data Structures", "Scheduled", "#d81945");
        assertThat(minimal.html()).contains("© " + Year.now().getValue() + " Verdixa. All rights reserved.");
        assertThat(dashboard.html()).doesNotContain("localhost", "VERDIXA");
    }
}
