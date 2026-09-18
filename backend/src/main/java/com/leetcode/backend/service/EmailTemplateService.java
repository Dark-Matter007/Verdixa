package com.leetcode.backend.service;

import java.time.Year;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

/**
 * Email-safe Verdixa presentation layer. It intentionally contains no mail transport,
 * workflow, or authorization behaviour: callers pass already-authorized display values.
 */
public final class EmailTemplateService {
    public static final String LOGO_CONTENT_ID = "verdixa-email-logo";
    private static final String LOGO = "cid:" + LOGO_CONTENT_ID;
    private static final String FONT = "Inter,Arial,Helvetica,sans-serif";

    public record RenderedEmail(String html, String text) { }
    public record AssessmentDetails(String name, String dateTime, String duration, String accessCode,
                                    String mode, String status, List<String> instructions) { }
    public record DashboardDetails(String itemLabel, String itemName, String dateTime, String duration,
                                   String status, String quote) { }

    private EmailTemplateService() { }

    /** Design 2 — the red/black verification and high-importance security treatment. */
    public static RenderedEmail buildBoldGradientOtpEmail(String name, String heading, String explanation,
                                                           String otp, String expiry, String ctaLabel,
                                                           String ctaUrl, String securityMessage) {
        String safeOtp = safeDigits(otp);
        String button = ctaUrl == null || ctaUrl.isBlank() ? "" : boldButton(ctaUrl, ctaLabel);
        String fallback = ctaUrl == null || ctaUrl.isBlank() ? "" : urlFallback(ctaUrl, true);
        String html = document("#050505", ""
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#050505;background-image:linear-gradient(135deg,#050505 0%,#210008 45%,#6f001d 100%);\"><tr><td align=\"center\" style=\"padding:28px 12px;\">"
                + "<table role=\"presentation\" class=\"email-shell\" width=\"640\" cellpadding=\"0\" cellspacing=\"0\" style=\"width:100%;max-width:640px;background:#080808;background-image:radial-gradient(circle at 91% 8%,#d71945 0%,#80001f 14%,transparent 34%),linear-gradient(140deg,#050505 0%,#270009 57%,#73001f 100%);border:1px solid #c31540;border-radius:14px;overflow:hidden;\">"
                + "<tr><td style=\"padding:24px 30px 8px;\">" + boldBrandRow() + "</td></tr>"
                + "<tr><td align=\"center\" style=\"padding:27px 30px 22px;\"><h1 style=\"margin:0;color:#ffffff;font-family:" + FONT + ";font-size:29px;line-height:36px;font-weight:800;\">" + escape(heading) + "</h1><p style=\"margin:10px 0 0;color:#ffffff;font-family:" + FONT + ";font-size:15px;line-height:23px;\">" + escape(explanation) + "</p></td></tr>"
                + "<tr><td style=\"padding:0 18px 22px;\"><table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#070707;border:1px solid #b80e37;border-radius:10px;\"><tr><td style=\"padding:24px;\"><table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\"><tr><td width=\"42\" valign=\"top\"><div style=\"width:38px;height:38px;line-height:38px;text-align:center;background:#650017;border-radius:9px;color:#ffffff;font-size:21px;\">✉</div></td><td style=\"padding-left:10px;\"><strong style=\"color:#ffffff;font-family:" + FONT + ";font-size:16px;\">Your Verification Code</strong><br><span style=\"color:#ffffff;font-family:" + FONT + ";font-size:13px;\">Enter the code below to continue.</span></td></tr></table>"
                + otpCells(safeOtp)
                + "<p style=\"margin:17px 0 0;text-align:center;color:#ffffff;font-family:" + FONT + ";font-size:13px;\">◷&nbsp; Expires in " + escape(expiry) + "</p>"
                + (button.isEmpty() ? "" : "<div style=\"height:18px;line-height:18px\">&nbsp;</div>" + button)
                + "</td></tr></table></td></tr>"
                + "<tr><td style=\"padding:0 30px 20px;\">" + fallback + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#160005;background-image:linear-gradient(90deg,#120004,#460012);border:1px solid #8e092b;border-radius:8px;\"><tr><td style=\"padding:15px 16px;color:#ffffff;font-family:" + FONT + ";font-size:12px;line-height:18px;\"><strong style=\"color:#ffffff;\">Keep your account safe</strong><br>" + escape(securityMessage) + "</td></tr></table></td></tr>"
                + "<tr><td style=\"padding:0 30px 25px;\"><table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\"><tr><td width=\"25%\" align=\"center\" style=\"color:#ffffff;font-family:" + FONT + ";font-size:11px;line-height:17px;\">♙<br>Learn</td><td width=\"25%\" align=\"center\" style=\"color:#ffffff;font-family:" + FONT + ";font-size:11px;line-height:17px;\">♜<br>Compete</td><td width=\"25%\" align=\"center\" style=\"color:#ffffff;font-family:" + FONT + ";font-size:11px;line-height:17px;\">☆<br>Get Hired</td><td width=\"25%\" align=\"center\" style=\"color:#ffffff;font-family:" + FONT + ";font-size:11px;line-height:17px;\">↗<br>Grow</td></tr></table></td></tr>"
                + boldFooter()
                + "</table></td></tr></table>");
        return new RenderedEmail(html, "Verdixa\n\n" + plainGreeting(name) + "\n\n" + heading + "\n" + explanation
                + "\n\nYour verification code is: " + safeOtp + "\nThis code expires in " + expiry + ".\n\n"
                + securityMessage + plainUrl(ctaUrl));
    }

    /** Design 3 — structured assessment communications. */
    public static RenderedEmail buildEnterpriseAssessmentEmail(String name, String heading, String explanation,
                                                                AssessmentDetails details, String ctaLabel, String ctaUrl) {
        String html = document("#eef0f3", ""
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#eef0f3;\"><tr><td align=\"center\" style=\"padding:26px 12px;\">"
                + "<table role=\"presentation\" class=\"email-shell\" width=\"640\" cellpadding=\"0\" cellspacing=\"0\" style=\"width:100%;max-width:640px;background:#ffffff;border:1px solid #dfe3e8;border-radius:11px;overflow:hidden;\">"
                + "<tr><td style=\"padding:23px 28px 18px;\">" + lightBrandRow("Build. Assess. Grow.") + "</td></tr><tr><td style=\"border-top:1px solid #e5e7eb;font-size:1px;line-height:1px;\">&nbsp;</td></tr>"
                + "<tr><td style=\"padding:27px 28px 12px;\"><h1 style=\"margin:0 0 10px;color:#111827;font-family:" + FONT + ";font-size:24px;line-height:31px;\">Hello, " + escape(name) + ",</h1><p style=\"margin:0;color:#424b59;font-family:" + FONT + ";font-size:14px;line-height:22px;\">" + escape(explanation) + "</p></td></tr>"
                + "<tr><td style=\"padding:12px 28px 16px;\"><h2 style=\"margin:0 0 12px;color:#141820;font-family:" + FONT + ";font-size:20px;line-height:27px;\">" + escape(heading) + "</h2>" + assessmentRows(details) + "</td></tr>"
                + "<tr><td style=\"padding:0 28px 16px;\">" + redButton(ctaUrl, ctaLabel) + urlFallback(ctaUrl, false) + "</td></tr>"
                + "<tr><td style=\"padding:0 28px 22px;\">" + instructions(details.instructions()) + "<p style=\"margin:19px 0 0;color:#18202a;font-family:" + FONT + ";font-size:14px;line-height:22px;\"><strong>Good luck!</strong><br>The Verdixa Team</p></td></tr>"
                + lightFooter()
                + "</table></td></tr></table>");
        return new RenderedEmail(html, "Verdixa\n\n" + plainGreeting(name) + "\n\n" + heading + "\n" + explanation + "\n\n"
                + detailsText(details) + "\n\n" + ctaLabel + ": " + safeUrl(ctaUrl) + "\n\n" + instructionsText(details.instructions()));
    }

    /** Design 4 — sparse, high-contrast account messages. */
    public static RenderedEmail buildMinimalLuxuryEmail(String name, String heading, String explanation,
                                                         String ctaLabel, String ctaUrl, String code,
                                                         String smallPrint) {
        String action = ctaUrl == null || ctaUrl.isBlank() ? "" : darkButton(ctaUrl, ctaLabel);
        String codeHtml = code == null || code.isBlank() ? "" : "<p style=\"margin:25px 0 5px;color:#151a20;font-family:" + FONT + ";font-size:12px;letter-spacing:1px;text-transform:uppercase;\">Your code</p><p style=\"margin:0;color:#111827;font-family:" + FONT + ";font-size:29px;font-weight:800;letter-spacing:8px;\">" + escape(code) + "</p>";
        String html = document("#edf0f3", ""
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#edf0f3;\"><tr><td align=\"center\" style=\"padding:28px 12px;\">"
                + "<table role=\"presentation\" class=\"email-shell\" width=\"640\" cellpadding=\"0\" cellspacing=\"0\" style=\"width:100%;max-width:640px;background:#ffffff;border:1px solid #e4e6ea;border-radius:10px;overflow:hidden;\">"
                + "<tr><td style=\"padding:24px 30px 0;background:#ffffff;background-image:linear-gradient(145deg,transparent 0%,transparent 65%,#fff5f6 100%);\">" + lightBrandRow("A more skilled tomorrow.") + "</td></tr>"
                + "<tr><td align=\"center\" style=\"padding:49px 30px 38px;background:#ffffff;background-image:linear-gradient(150deg,transparent 0%,transparent 75%,#fff1f3 100%);\"><h1 style=\"margin:0;color:#0f1217;font-family:" + FONT + ";font-size:28px;line-height:35px;font-weight:800;\">" + escape(heading) + "</h1><p style=\"max-width:410px;margin:13px auto 0;color:#515966;font-family:" + FONT + ";font-size:15px;line-height:23px;\">" + escape(explanation) + "</p>" + codeHtml + (action.isEmpty() ? "" : "<div style=\"height:25px;line-height:25px\">&nbsp;</div>" + action) + "<p style=\"margin:20px 0 0;color:#7d8590;font-family:" + FONT + ";font-size:12px;line-height:19px;\">" + escape(smallPrint) + "</p>" + (action.isEmpty() ? "" : urlFallback(ctaUrl, false)) + "</td></tr>"
                + "<tr><td style=\"padding:0 30px;\"><div style=\"border-top:1px solid #e4e7eb;font-size:1px;line-height:1px\">&nbsp;</div></td></tr>"
                + "<tr><td align=\"center\" style=\"padding:20px 30px 25px;color:#7b8490;font-family:" + FONT + ";font-size:12px;line-height:19px;\">If you didn't request this, you can safely ignore this email.</td></tr>"
                + lightFooter()
                + "</table></td></tr></table>");
        return new RenderedEmail(html, "Verdixa\n\n" + plainGreeting(name) + "\n\n" + heading + "\n" + explanation
                + (code == null || code.isBlank() ? "" : "\n\nYour code is: " + code) + "\n\n" + smallPrint + plainUrl(ctaUrl));
    }

    /** Design 5 — an email-scale Verdixa dashboard. */
    public static RenderedEmail buildDashboardEmail(String name, String heading, String explanation,
                                                     DashboardDetails details, String ctaLabel, String ctaUrl) {
        String html = document("#e8ebee", ""
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#e8ebee;\"><tr><td align=\"center\" style=\"padding:26px 12px;\">"
                + "<table role=\"presentation\" class=\"email-shell\" width=\"660\" cellpadding=\"0\" cellspacing=\"0\" style=\"width:100%;max-width:660px;background:#ffffff;border:1px solid #dce1e5;border-radius:11px;overflow:hidden;\">"
                + "<tr><td style=\"padding:20px 28px 24px;background:#180c13;background-image:radial-gradient(circle at 83% 0%,#e5657c 0%,#8e1834 18%,transparent 45%),linear-gradient(135deg,#111216 0%,#3d1423 58%,#821a35 100%);\">" + darkBrandRow() + "<div align=\"center\" style=\"padding-top:24px;\"><h1 style=\"margin:0;color:#ffffff;font-family:" + FONT + ";font-size:27px;line-height:34px;font-weight:800;\">" + escape(heading) + "</h1><p style=\"margin:7px 0 0;color:#fbe5e9;font-family:" + FONT + ";font-size:14px;line-height:21px;\">" + escape(explanation) + "</p></div></td></tr>"
                + "<tr><td style=\"padding:16px 18px 8px;\">" + dashboardCards(details) + "</td></tr>"
                + "<tr><td style=\"padding:0 18px 17px;\"><table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#fff1f3;border-radius:7px;\"><tr><td style=\"padding:13px 16px;color:#c0183a;font-family:" + FONT + ";font-size:13px;line-height:19px;\"><strong style=\"font-size:20px;vertical-align:middle;\">“</strong>&nbsp; " + escape(details.quote()) + "</td></tr></table></td></tr>"
                + "<tr><td align=\"center\" style=\"padding:0 28px 22px;\">" + redButton(ctaUrl, ctaLabel) + urlFallback(ctaUrl, false) + "</td></tr>"
                + lightFooter()
                + "</table></td></tr></table>");
        return new RenderedEmail(html, "Verdixa\n\n" + plainGreeting(name) + "\n\n" + heading + "\n" + explanation + "\n\n"
                + details.itemLabel() + ": " + details.itemName() + "\nDate & Time: " + details.dateTime()
                + "\nDuration: " + details.duration() + "\nStatus: " + details.status() + "\n\n"
                + ctaLabel + ": " + safeUrl(ctaUrl));
    }

    /** Used only by the local preview test; production mail always uses the CID logo. */
    public static String forPreview(String html, byte[] logo, String mimeType) {
        return html.replace(LOGO, "data:" + escapeAttribute(mimeType) + ";base64," + Base64.getEncoder().encodeToString(logo));
    }

    private static String document(String background, String body) {
        return "<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><meta name=\"x-apple-disable-message-reformatting\"><meta name=\"color-scheme\" content=\"light\"><meta name=\"supported-color-schemes\" content=\"light\"><style>body{margin:0!important;padding:0!important;background:" + background + ";}table{border-spacing:0;}img{border:0;line-height:100%;outline:none;text-decoration:none;}a{color:inherit;}@media only screen and (max-width:600px){.email-shell{width:100%!important;border-radius:0!important}.mobile-block{display:block!important;width:100%!important;box-sizing:border-box!important}.mobile-pad{padding-left:18px!important;padding-right:18px!important}.otp-cell{width:14%!important;font-size:22px!important}.hide-mobile{display:none!important}}</style></head><body>" + body + "</body></html>";
    }

    private static String darkBrandRow() {
        return "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\"><tr><td valign=\"middle\"><img src=\"" + LOGO + "\" width=\"118\" alt=\"Verdixa\" style=\"display:block;width:118px;max-width:100%;height:auto;\"></td><td class=\"hide-mobile\" align=\"right\" valign=\"middle\" style=\"color:#fbe4e9;font-family:" + FONT + ";font-size:11px;font-weight:700;\">Build. Assess. Grow.</td></tr></table>";
    }
    private static String boldBrandRow() {
        return "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\"><tr><td valign=\"middle\"><img src=\"" + LOGO + "\" width=\"118\" alt=\"Verdixa\" style=\"display:block;width:118px;max-width:100%;height:auto;\"></td><td class=\"hide-mobile\" align=\"right\" valign=\"middle\" style=\"color:#ffffff;font-family:" + FONT + ";font-size:11px;font-weight:700;\">Build. Assess. Grow.</td></tr></table>";
    }
    private static String lightBrandRow(String note) {
        return "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\"><tr><td valign=\"middle\"><img src=\"" + LOGO + "\" width=\"118\" alt=\"Verdixa\" style=\"display:block;width:118px;max-width:100%;height:auto;\"></td><td class=\"hide-mobile\" align=\"right\" valign=\"middle\" style=\"color:#7a818c;font-family:" + FONT + ";font-size:11px;\">" + escape(note) + "</td></tr></table>";
    }
    private static String darkFooter() {
        return "<tr><td style=\"padding:21px 30px;background:#070809;border-top:1px solid #3b2028;\">" + darkBrandRow() + "<p style=\"margin:11px 0 0;color:#bfc4cb;font-family:" + FONT + ";font-size:11px;line-height:17px;\">Skills today. A brighter tomorrow.</p><p style=\"margin:17px 0 0;color:#9299a2;font-family:" + FONT + ";font-size:10px;\">© " + Year.now().getValue() + " Verdixa. All rights reserved.</p></td></tr>";
    }
    private static String boldFooter() {
        return "<tr><td style=\"padding:21px 30px;background:#050505;background-image:linear-gradient(90deg,#050505,#250009);border-top:1px solid #9e1031;\">" + boldBrandRow() + "<p style=\"margin:11px 0 0;color:#ffffff;font-family:" + FONT + ";font-size:11px;line-height:17px;\">Skills today. A brighter tomorrow.</p><p style=\"margin:17px 0 0;color:#ffffff;font-family:" + FONT + ";font-size:10px;\">© " + Year.now().getValue() + " Verdixa. All rights reserved.</p></td></tr>";
    }
    private static String lightFooter() {
        return "<tr><td style=\"padding:19px 28px;background:#ffffff;border-top:1px solid #e3e6ea;\">" + lightBrandRow("Skills for what's next.") + "<p style=\"margin:11px 0 0;color:#8a919b;font-family:" + FONT + ";font-size:10px;\">© " + Year.now().getValue() + " Verdixa. All rights reserved.</p></td></tr>";
    }
    private static String otpCells(String otp) {
        StringBuilder cells = new StringBuilder("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"4\" style=\"margin-top:21px;\"><tr>");
        for (int index = 0; index < otp.length(); index++) cells.append("<td class=\"otp-cell\" align=\"center\" style=\"width:").append(100 / otp.length()).append("%;padding:14px 0;background:#050505;border:1px solid #d71945;border-radius:7px;color:#ffffff;font-family:").append(FONT).append(";font-size:25px;line-height:25px;font-weight:800;\">").append(escape(String.valueOf(otp.charAt(index)))).append("</td>");
        return cells.append("</tr></table><span style=\"display:none\">Your full verification code is ").append(escape(otp)).append(".</span>").toString();
    }
    private static String assessmentRows(AssessmentDetails d) {
        return "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"border:1px solid #e5e8ed;border-radius:8px;overflow:hidden;\">"
                + row("▧", "Assessment", d.name()) + row("▣", "Date & Time", d.dateTime()) + row("◷", "Duration", d.duration())
                + (blank(d.accessCode()) ? "" : row("⌁", "Access Code", d.accessCode())) + (blank(d.mode()) ? "" : row("▣", "Mode", d.mode()))
                + "</table>";
    }
    private static String row(String icon, String label, String value) {
        return "<tr><td width=\"31\" style=\"padding:12px 0 12px 13px;border-bottom:1px solid #e9ecf0;color:#ec173d;font-family:" + FONT + ";font-size:16px;\">" + icon + "</td><td width=\"30%\" style=\"padding:12px 8px;border-bottom:1px solid #e9ecf0;color:#3f4753;font-family:" + FONT + ";font-size:12px;font-weight:700;\">" + escape(label) + "</td><td style=\"padding:12px 12px 12px 0;border-bottom:1px solid #e9ecf0;color:#1d2530;font-family:" + FONT + ";font-size:12px;line-height:18px;font-weight:600;\">" + escape(value) + "</td></tr>";
    }
    private static String instructions(List<String> items) {
        if (items == null || items.isEmpty()) return "";
        StringBuilder list = new StringBuilder("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#f4f5f7;border-radius:8px;\"><tr><td style=\"padding:15px 17px;color:#303846;font-family:").append(FONT).append(";font-size:12px;line-height:19px;\"><strong style=\"color:#1e2530;\">Important Instructions</strong><ul style=\"margin:8px 0 0;padding-left:18px;\">");
        for (String item : items) list.append("<li>").append(escape(item)).append("</li>");
        return list.append("</ul></td></tr></table>").toString();
    }
    private static String dashboardCards(DashboardDetails d) {
        return "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"5\"><tr>"
                + dashboardCard("▧", d.itemLabel(), d.itemName(), "") + dashboardCard("▣", "Date & Time", d.dateTime(), "")
                + dashboardCard("◷", "Duration", d.duration(), "") + dashboardCard("✓", "Status", d.status(), "status") + "</tr></table>";
    }
    private static String dashboardCard(String icon, String label, String value, String kind) {
        String valueHtml = "status".equals(kind) ? "<span style=\"display:inline-block;padding:5px 10px;background:#dff6e6;border-radius:7px;color:#19733b;font-size:11px;\">" + escape(value) + "</span>" : escape(value);
        return "<td class=\"mobile-block\" width=\"25%\" valign=\"top\" style=\"padding:12px 9px;background:#ffffff;border:1px solid #e7eaee;border-radius:7px;\"><table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\"><tr><td valign=\"top\" style=\"padding-right:8px;color:#ec173d;font-size:17px;\">" + icon + "</td><td><span style=\"display:block;color:#858d98;font-family:" + FONT + ";font-size:10px;line-height:14px;\">" + escape(label) + "</span><span style=\"display:block;margin-top:2px;color:#202733;font-family:" + FONT + ";font-size:11px;line-height:16px;font-weight:700;\">" + valueHtml + "</span></td></tr></table></td>";
    }
    private static String redButton(String url, String label) { return button(url, label, "#b91037;background-image:linear-gradient(90deg,#9e1031,#d81945 55%,#ef526c)", "#ffffff", "8px"); }
    private static String boldButton(String url, String label) { return button(url, label, "#9e1031;background-image:linear-gradient(90deg,#79001e,#cc123d 55%,#f01b4c)", "#ffffff", "8px"); }
    private static String darkButton(String url, String label) { return button(url, label, "#151a20", "#ffffff", "7px"); }
    private static String button(String url, String label, String background, String color, String radius) {
        return "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\"><tr><td align=\"center\" style=\"border-radius:" + radius + ";background:" + background + ";\"><a href=\"" + escapeAttribute(url) + "\" style=\"display:block;padding:15px 18px;color:" + color + ";font-family:" + FONT + ";font-size:14px;font-weight:800;line-height:18px;text-align:center;text-decoration:none;\">" + escape(label) + " &nbsp;→</a></td></tr></table>";
    }
    private static String urlFallback(String url, boolean dark) {
        if (blank(url)) return "";
        return "<p style=\"margin:14px 0 0;color:" + (dark ? "#ffffff" : "#727b87") + ";font-family:" + FONT + ";font-size:10px;line-height:16px;word-break:break-word;\">If the button doesn't work, copy and paste this URL into your browser:<br><a href=\"" + escapeAttribute(url) + "\" style=\"color:" + (dark ? "#ffffff" : "#b81131") + ";\">" + escape(url) + "</a></p>";
    }
    private static String detailsText(AssessmentDetails d) {
        return "Assessment: " + d.name() + "\nDate & Time: " + d.dateTime() + "\nDuration: " + d.duration()
                + (blank(d.accessCode()) ? "" : "\nAccess Code: " + d.accessCode()) + (blank(d.mode()) ? "" : "\nMode: " + d.mode());
    }
    private static String instructionsText(List<String> items) { return items == null || items.isEmpty() ? "" : "Important instructions:\n- " + String.join("\n- ", items); }
    private static String plainGreeting(String name) { return blank(name) ? "Hello," : "Hello " + name + ","; }
    private static String plainUrl(String url) { return blank(url) ? "" : "\n\nContinue: " + safeUrl(url); }
    private static String safeUrl(String url) { return blank(url) ? "" : url; }
    private static String safeDigits(String value) { String result = value == null ? "" : value.replaceAll("[^0-9A-Za-z]", ""); return result.isEmpty() ? "—" : result; }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static String escape(String value) { return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;"); }
    private static String escapeAttribute(String value) { return escape(value); }
}
