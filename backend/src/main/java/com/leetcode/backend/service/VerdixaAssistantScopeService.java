package com.leetcode.backend.service;

import com.leetcode.backend.dto.AssistantChatRequest;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;

/** Shared intent and security policy. Role-specific services remain the authorization authority. */
@Service
public class VerdixaAssistantScopeService {
    private static final String OUT_OF_SCOPE = "I'm the Verdixa Assistant, so I can only help with Verdixa-related questions and features.";
    private static final String SENSITIVE = "I can't expose passwords, OTPs, tokens, secrets, credentials, hashes, hidden judge data, or configuration values.";
    private static final String OVERVIEW = "Verdixa is a coding-practice and competitive-programming platform. You can solve Java, C++, and Python problems, run and submit code to the online judge, join contests, use progressive hints and editorials, follow the leaderboard and daily challenge, and earn milestone certificates. You can create an account with email or continue with Google or GitHub.";
    private static final String BENEFITS = "Verdixa gives you a structured place to practice coding problems, submit solutions through a Java, C++, and Python judge, track progress, join contests, use progressive hints and editorials, follow rankings, complete daily challenges, and earn certificates at problem-solving milestones. It is designed to help you improve problem-solving skill and practice consistency.";
    private static final String UNKNOWN_HELP = "I can help with Verdixa features, account access, coding problems, submissions, contests, certificates, hints, editorials, and your progress after sign-in. What would you like to know?";
    private static final Set<String> PAGE_TYPES = Set.of("LANDING", "DASHBOARD", "PROBLEM", "CONTEST", "ADMIN", "PAGE");

    private final GeminiClient gemini;

    public VerdixaAssistantScopeService(GeminiClient gemini) {
        this.gemini = gemini;
    }

    public VerdixaAssistantScope classify(String rawMessage) {
        return classify(rawMessage, null, VerdixaAssistantAudience.PUBLIC);
    }

    public VerdixaAssistantScope classify(AssistantChatRequest request, VerdixaAssistantAudience audience) {
        return classify(request.message(), request.pageType(), audience);
    }

    public VerdixaAssistantScope classify(String rawMessage, String rawPageType, VerdixaAssistantAudience audience) {
        String q = normalize(rawMessage);
        String pageType = normalizePageType(rawPageType);

        if (isSensitive(q)) return VerdixaAssistantScope.SENSITIVE_REQUEST;
        if (isPromptInjection(q)) return VerdixaAssistantScope.OUT_OF_SCOPE;

        VerdixaAssistantScope personal = personalIntent(q);
        if (personal != null) return personal;

        if (isAdminAction(q, audience)) return VerdixaAssistantScope.ADMIN_ACTION;
        if (isExplicitAdminData(q)) return VerdixaAssistantScope.ADMIN_DATA;
        if (audience == VerdixaAssistantAudience.ADMIN && isAdminSummary(q, pageType)) return VerdixaAssistantScope.ADMIN_SUMMARY;

        if (isBenefitsQuestion(q, pageType)) return VerdixaAssistantScope.PUBLIC_BENEFITS;
        if (publicHelpAnswer(q, pageType) != null) return VerdixaAssistantScope.PUBLIC_HELP;
        if (isObviousOutsideTopic(q)) return VerdixaAssistantScope.OUT_OF_SCOPE;

        if (hasVerdixaContext(q, pageType)) {
            return isNavigationQuestion(q) ? VerdixaAssistantScope.VERDIXA_NAVIGATION : VerdixaAssistantScope.UNKNOWN_VERDIXA_HELP;
        }

        return gemini.classifyIntent(q, pageType, audience).orElse(VerdixaAssistantScope.OUT_OF_SCOPE);
    }

    public String publicHelpAnswer(String rawMessage) {
        return publicHelpAnswer(rawMessage, null);
    }

    public String publicHelpAnswer(String rawMessage, String rawPageType) {
        String q = normalize(rawMessage);
        String pageType = normalizePageType(rawPageType);
        if (isBenefitsQuestion(q, pageType)) return BENEFITS;
        if (isOverviewQuestion(q, pageType)) return OVERVIEW;
        if (containsAny(q, "sign up", "signup", "create account", "create an account", "how can i register", "join verdixa")) return "Click Sign up on Verdixa, enter your details, verify your email with the OTP, then sign in. You can also choose Continue with Google or Continue with GitHub to create or access an account directly.";
        if (containsAny(q, "google", "github", "social login") && containsAny(q, "login", "log in", "sign in", "account", "can i use", "continue with", "how to")) return "Choose Continue with Google or Continue with GitHub on the Sign in or Sign up page. Verdixa verifies the provider identity and safely links or creates the account.";
        if (containsAny(q, "log in", "login", "sign in")) return "Open Sign in and use your Verdixa username and password, or choose Continue with Google or Continue with GitHub.";
        if (containsAny(q, "forgot", "forget", "reset") && containsAny(q, "password", "passowrd", "passcode")) return "Select Forgot password on the sign-in page. Verdixa sends a short-lived reset code when an account exists for that email, then lets you choose a new password.";
        if (containsAny(q, "verify email", "email verification", "verification code", "otp", "didnt get", "resend verification", "resend code")) return "After email/password signup, enter the six-digit verification OTP from Verdixa. If it has not arrived, use the resend option; social sign-in accounts with a verified provider email do not need this OTP.";
        if (isProblemHelp(q)) return "Verdixa problems are coding exercises with Easy, Medium, and Hard difficulties. Sign in, choose a problem, write Java, C++, or Python, run custom cases, and submit to the judge.";
        if (isLanguageHelp(q)) return "Verdixa supports Java, C++, and Python for running and submitting programming problems.";
        if (isJudgeHelp(q)) return "Run Code checks custom cases before submission. Submit evaluates the solution against official judge cases, including hidden cases where configured, and returns a verdict such as Accepted, Wrong Answer, Runtime Error, or TLE.";
        if (q.contains("contest")) return "Verdixa contests are scheduled coding events. Sign in to register, then solve the available contest problems during the defined window; access, timing, and standings are enforced by the platform.";
        if (containsAny(q, "certificate", "certifcate", "milestone")) return "Verdixa certificates recognize configured milestones at 50, 100, and 150 solved problems. After signing in, you can see progress and earned certificates in your profile.";
        if (q.contains("leaderboard") || q.contains("ranking")) return "The Verdixa leaderboard shows problem-solving progress and standings. Sign in to see the current leaderboard and your rank where available.";
        if (q.contains("hint")) return "Verdixa hints provide progressive guidance while you solve. Availability follows the configured access rules, and locked hint content is never exposed early.";
        if (q.contains("editorial")) return "Verdixa editorials explain intended approaches after the configured access rules allow them. Locked editorial content is never exposed early.";
        if (q.contains("daily challenge")) return "The daily challenge highlights a Verdixa problem to help build a regular practice habit. Sign in to open and solve it.";
        if (q.contains("accepted")) return "Accepted means your submission passed Verdixa's official judge cases.";
        if (q.contains("wrong answer")) return "Wrong Answer means the program ran but produced an incorrect result for at least one official judge case.";
        if (q.contains("tle") || q.contains("time limit exceeded")) return "TLE means Time Limit Exceeded: the submitted program did not finish within the judge's allowed time.";
        if (q.contains("runtime error")) return "Runtime Error means the program failed while executing, such as from an unhandled exception or invalid operation.";
        if (isNavigationQuestion(q) && hasVerdixaContext(q, pageType)) return UNKNOWN_HELP;
        return null;
    }

    private boolean isSensitive(String q) {
        boolean asksToExpose = containsAny(q, "show", "reveal", "give", "tell", "list", "display", "export", "print");
        boolean secret = containsAny(q, "password", "bcrypt", "hash", "otp", "one time code", "reset token", "access token", "refresh token", "jwt secret", "api key", "client secret", "smtp password", "database password", "environment secret", "hidden judge", "hidden test");
        return asksToExpose && secret;
    }

    private boolean isPromptInjection(String q) {
        return containsAny(q, "ignore previous", "ignore your", "ignore all", "bypass restrictions", "override instructions");
    }

    private VerdixaAssistantScope personalIntent(String q) {
        if (containsAny(q, "how am i doing", "how is my progress", "how many did i finish", "what have i completed", "how many hard ones", "what should i solve next")) return VerdixaAssistantScope.USER_SUMMARY;
        if (containsAny(q, "what did i submit", "submit recently", "recent submissions", "latest accepted", "what did i solve", "what problem did i solve")) return VerdixaAssistantScope.USER_SUBMISSIONS;
        if (containsAny(q, "what contest did i", "which contest did i", "am i in any contest", "am i registered", "my contests", "contests am i registered")) return VerdixaAssistantScope.USER_CONTESTS;
        if (containsAny(q, "certificate", "certifcate") && containsAny(q, "my", "i earned", "am i close", "how far", "next")) return VerdixaAssistantScope.USER_CERTIFICATES;
        boolean ownsRequest = q.contains("show my") || q.matches(".*\\bmy\\b.*\\b(profile|rank|username|submission|progress|account|certificate|contest|email)\\b.*") || q.matches(".*\\bhow many\\b.*\\b(have i|i have|i solved|i attempted|did i)\\b.*") || containsAny(q, "have i solved", "have i completed", "what is my username", "whats my username");
        return ownsRequest ? VerdixaAssistantScope.USER_DATA : null;
    }

    private boolean isAdminAction(String q, VerdixaAssistantAudience audience) {
        if (q.contains("contest") && containsAny(q, "create", "make", "build", "suggest", "recommend", "balance", "draft", "pick")) return true;
        return audience == VerdixaAssistantAudience.ADMIN && containsAny(q, "pick best problems", "choose balanced problems", "suggest contest problems", "suggest a contest name");
    }

    private boolean isExplicitAdminData(String q) {
        return containsAny(q, "how many users", "how many people use", "registered users", "all usernames", "user emails", "all user emails", "contest participants", "admin analytics", "admin contest statistics", "user activity", "users performing", "each participant", "most registrations", "recent contest activity", "solve rate", "acceptance rate", "issued certificates") ||
                (q.contains("contest") && containsAny(q, "active contests", "contests are active", "upcoming contests", "completed contests"));
    }

    private boolean isAdminSummary(String q, String pageType) {
        return containsAny(q, "how is the platform doing", "how is platform doing", "how is verdixa doing", "platform summary") || ("ADMIN".equals(pageType) && containsAny(q, "how are we doing", "what can i do here"));
    }

    private boolean isBenefitsQuestion(String q, String pageType) {
        boolean explicitProduct = q.contains("verdixa") || q.contains("this platform") || q.contains("this coding platform");
        boolean benefits = containsAny(q, "why should i use", "what will i get", "what do i get", "what can i get", "benefit", "help me improve", "help me with coding", "help students", "help me", "do for me", "is verdixa useful", "useful for coding", "what can i learn", "after joining", "after creating account", "after i join");
        boolean landingReference = "LANDING".equals(pageType) && containsAny(q, "how it help me", "how can this help me", "what is this for", "what can i do here", "what does this platform provide", "what do i get", "what can i get from this");
        return (explicitProduct && benefits) || landingReference || q.contains("what can i learn here");
    }

    private boolean isOverviewQuestion(String q, String pageType) {
        boolean namesProduct = q.contains("verdixa");
        boolean overviewLanguage = containsAny(q, "what is", "wat is", "whats", "whts", "tell me", "help me kno", "explain", "overview", "feature", "how does", "how verdixa work", "how is verdixa doing", "what can verdixa", "what does verdixa", "is verdixa");
        boolean pageReference = containsAny(q, "this platform", "this website", "this coding platform", "what can i do here", "what does this platform provide");
        return (namesProduct && overviewLanguage) || pageReference || ("LANDING".equals(pageType) && containsAny(q, "what can i do here", "what is this for", "how is platform doing"));
    }

    private boolean isProblemHelp(String q) {
        return containsAny(q, "what are problems", "how problem work", "how do problems", "how to solve problems", "solve problems", "start solving", "problem difficulty", "what difficulties");
    }

    private boolean isLanguageHelp(String q) {
        return (q.contains("verdixa") && containsAny(q, "language", "java", "c++", "python")) || (q.contains("language") && containsAny(q, "support", "can i use", "available"));
    }

    private boolean isJudgeHelp(String q) {
        return containsAny(q, "run code", "how run", "how submit", "problem submit", "how judge", "online judge", "submission work");
    }

    private boolean hasVerdixaContext(String q, String pageType) {
        return q.contains("verdixa") || containsAny(q, "this platform", "this website", "coding platform") || (!pageType.isBlank() && containsAny(q, "here", "this", "on this page"));
    }

    private boolean isNavigationQuestion(String q) {
        return containsAny(q, "where", "go to", "open", "find", "navigate", "how do i start", "how to start");
    }

    private boolean isObviousOutsideTopic(String q) {
        return containsAny(q, "prime minister", "weather", "cricket news", "write a poem", "black hole", "black holes", "recommend a movie", "stock market", "quantum computing", "capital of", "latest news", "who is elon", "what is python", "teach me python");
    }

    private static boolean containsAny(String value, String... candidates) {
        for (String candidate : candidates) if (value.contains(candidate)) return true;
        return false;
    }

    private String normalizePageType(String value) {
        if (value == null) return "";
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return PAGE_TYPES.contains(normalized) ? normalized : "";
    }

    public String outOfScopeMessage() { return OUT_OF_SCOPE; }
    public String sensitiveMessage() { return SENSITIVE; }
    public String benefitsMessage() { return BENEFITS; }
    public String unknownVerdixaMessage() { return UNKNOWN_HELP; }
    public String loginRequiredMessage() { return "Sign in to Verdixa and I can check your actual solved problems, submissions, contest registrations, rank, profile, and certificate progress."; }
    public String adminRequiredMessage() { return "That information or action requires authorized Verdixa administration access."; }

    public String normalize(String value) {
        if (value == null) return "";
        return value.trim().toLowerCase(Locale.ROOT)
                .replace('’', '\'')
                .replaceAll("(?<=\\w)'(?=\\w)", "")
                .replaceAll("[^a-z0-9+\\-]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
