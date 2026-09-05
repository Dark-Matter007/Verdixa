package com.leetcode.backend.service;

import com.leetcode.backend.dto.DailyChallengeAdminResponse;
import com.leetcode.backend.dto.DailyChallengeResponse;
import com.leetcode.backend.model.DailyChallenge;
import com.leetcode.backend.model.Problem;
import com.leetcode.backend.model.Submission;
import com.leetcode.backend.model.User;
import com.leetcode.backend.repository.DailyChallengeRepository;
import com.leetcode.backend.repository.ProblemRepository;
import com.leetcode.backend.repository.SubmissionRepository;
import com.leetcode.backend.repository.UserRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DailyChallengeService {
    private final DailyChallengeRepository challenges;
    private final ProblemRepository problems;
    private final SubmissionRepository submissions;
    private final UserRepository users;

    public DailyChallengeService(DailyChallengeRepository challenges, ProblemRepository problems,
            SubmissionRepository submissions, UserRepository users) {
        this.challenges = challenges;
        this.problems = problems;
        this.submissions = submissions;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public Optional<DailyChallengeResponse> today(String username) {
        User user = user(username);
        return challenges.findByChallengeDate(today()).filter(challenge -> challenge.getProblem() != null)
                .map(challenge -> response(challenge, user));
    }

    @Transactional(readOnly = true)
    public List<DailyChallengeResponse> history(String username) {
        LocalDate today = today();
        User user = user(username);
        return challenges.findByChallengeDateBetweenOrderByChallengeDateDesc(today.minusDays(365), today.minusDays(1))
                .stream().filter(challenge -> challenge.getProblem() != null)
                .map(challenge -> response(challenge, user)).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Integer> summary(String username) {
        LocalDate today = today();
        User user = user(username);
        List<DailyChallenge> history = challenges.findByChallengeDateBetweenOrderByChallengeDateDesc(
                today.minusDays(365), today);
        Set<LocalDate> completed = completedDates(user, history);
        return Map.of("completed", completed.size(), "currentStreak", streak(completed, today),
                "longestStreak", longest(completed));
    }

    @Transactional(readOnly = true)
    public List<DailyChallengeAdminResponse> adminHistory() {
        return challenges.findAllByOrderByChallengeDateDesc().stream()
                .filter(challenge -> challenge.getProblem() != null).map(this::adminResponse).toList();
    }

    @Transactional
    public DailyChallengeResponse schedule(DailyChallenge requested) {
        if (requested.getChallengeDate() == null || requested.getProblem() == null
                || requested.getProblem().getId() == null) {
            throw new IllegalArgumentException("Challenge date and problem are required.");
        }
        if (challenges.findByChallengeDate(requested.getChallengeDate()).isPresent()) {
            throw new IllegalArgumentException("A challenge already exists for this date.");
        }
        requested.setProblem(publishedProblem(requested.getProblem().getId()));
        return DailyChallengeResponse.from(challenges.save(requested), false);
    }

    @Transactional
    public DailyChallengeResponse updateFuture(Long id, DailyChallenge requested) {
        DailyChallenge challenge = challenges.findById(id).orElseThrow();
        if (!challenge.getChallengeDate().isAfter(today())) {
            throw new IllegalArgumentException("Historical challenges are immutable.");
        }
        if (requested.getChallengeDate() != null && !requested.getChallengeDate().equals(challenge.getChallengeDate())
                && challenges.findByChallengeDate(requested.getChallengeDate()).isPresent()) {
            throw new IllegalArgumentException("A challenge already exists for this date.");
        }
        if (requested.getChallengeDate() != null) challenge.setChallengeDate(requested.getChallengeDate());
        if (requested.getProblem() != null && requested.getProblem().getId() != null) {
            challenge.setProblem(publishedProblem(requested.getProblem().getId()));
        }
        return DailyChallengeResponse.from(challenges.save(challenge), false);
    }

    @Transactional
    public void deleteFuture(Long id) {
        DailyChallenge challenge = challenges.findById(id).orElseThrow();
        if (!challenge.getChallengeDate().isAfter(today())) {
            throw new IllegalArgumentException("Historical challenges are immutable.");
        }
        challenges.delete(challenge);
    }

    private DailyChallengeResponse response(DailyChallenge challenge, User user) {
        boolean completed = submissionsFor(user, challenge).stream()
                .anyMatch(submission -> "ACCEPTED".equals(submission.getStatus()));
        return DailyChallengeResponse.from(challenge, completed);
    }

    private DailyChallengeAdminResponse adminResponse(DailyChallenge challenge) {
        List<Submission> entries = submissions.findByProblemIdOrderBySubmittedAtDesc(challenge.getProblem().getId())
                .stream().filter(submission -> submittedOn(submission, challenge.getChallengeDate())).toList();
        long participants = entries.stream().map(submission -> submission.getUser().getId()).distinct().count();
        long completed = entries.stream().filter(submission -> "ACCEPTED".equals(submission.getStatus()))
                .map(submission -> submission.getUser().getId()).distinct().count();
        return new DailyChallengeAdminResponse(challenge.getId(), challenge.getChallengeDate(),
                DailyChallengeResponse.from(challenge, false).problem(), participants, completed,
                participants == 0 ? 0 : Math.round(completed * 100.0 / participants));
    }

    private Set<LocalDate> completedDates(User user, List<DailyChallenge> values) {
        Set<LocalDate> dates = new HashSet<>();
        values.stream().filter(challenge -> challenge.getProblem() != null).forEach(challenge -> {
            if (submissionsFor(user, challenge).stream().anyMatch(s -> "ACCEPTED".equals(s.getStatus()))) {
                dates.add(challenge.getChallengeDate());
            }
        });
        return dates;
    }

    private List<Submission> submissionsFor(User user, DailyChallenge challenge) {
        return submissions.findByUserIdAndProblemIdOrderBySubmittedAtDesc(user.getId(), challenge.getProblem().getId())
                .stream().filter(submission -> submittedOn(submission, challenge.getChallengeDate())).toList();
    }

    private boolean submittedOn(Submission submission, LocalDate date) {
        return submission.getSubmittedAt() != null && submission.getSubmittedAt().toLocalDate().equals(date);
    }

    private int streak(Set<LocalDate> dates, LocalDate day) {
        if (!dates.contains(day)) day = day.minusDays(1);
        int count = 0;
        while (dates.contains(day)) { count++; day = day.minusDays(1); }
        return count;
    }

    private int longest(Set<LocalDate> dates) {
        int best = 0;
        for (LocalDate date : dates) {
            int count = 1;
            for (LocalDate cursor = date.minusDays(1); dates.contains(cursor); cursor = cursor.minusDays(1)) count++;
            best = Math.max(best, count);
        }
        return best;
    }

    private User user(String username) { return users.findByUsername(username).orElseThrow(); }
    private Problem publishedProblem(Long id) {
        Problem problem = problems.findById(id).orElseThrow();
        if (!problem.isActive()) throw new IllegalArgumentException("Only published problems can be scheduled.");
        return problem;
    }
    private LocalDate today() { return LocalDate.now(ZoneId.systemDefault()); }
}
