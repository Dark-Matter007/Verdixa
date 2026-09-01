package com.leetcode.backend.service;

import com.leetcode.backend.dto.LeaderboardEntryResponse;
import com.leetcode.backend.dto.UserProgressResponse;
import com.leetcode.backend.dto.UserResponse;
import com.leetcode.backend.model.Problem;
import com.leetcode.backend.model.Role;
import com.leetcode.backend.model.Submission;
import com.leetcode.backend.model.User;
import com.leetcode.backend.repository.ProblemRepository;
import com.leetcode.backend.repository.SubmissionRepository;
import com.leetcode.backend.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ProblemRepository problemRepository;
    private final SubmissionRepository submissionRepository;

    public UserService(
            UserRepository userRepository,
            ProblemRepository problemRepository,
            SubmissionRepository submissionRepository) {
        this.userRepository = userRepository;
        this.problemRepository = problemRepository;
        this.submissionRepository = submissionRepository;
    }

    public List<UserResponse> getAllUsers() {

        return userRepository.findAll()
                .stream()
                .map(UserResponse::fromUser)
                .toList();
    }

    public List<UserResponse> searchUsers(String query) {

        String search = query.toLowerCase();

        return userRepository.findAll()
                .stream()
                .filter(user ->
                        user.getUsername()
                                .toLowerCase()
                                .contains(search)
                        ||
                        user.getEmail()
                                .toLowerCase()
                                .contains(search)
                )
                .map(UserResponse::fromUser)
                .toList();
    }

    public UserResponse getUserById(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );

        return UserResponse.fromUser(user);
    }

    public UserResponse updateRole(Long id, Role role) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );

        user.setRole(role);

        User updatedUser = userRepository.save(user);

        return UserResponse.fromUser(updatedUser);
    }

    @Transactional(readOnly = true)
    public UserProgressResponse getProgressForUsername(String username) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return buildProgress(user, problemRepository.findByActiveTrue(),
                submissionRepository.findByUserIdOrderBySubmittedAtDesc(user.getId()));
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntryResponse> getLeaderboard() {

        List<Problem> activeProblems = problemRepository.findByActiveTrue();
        Set<Long> activeProblemIds = activeProblems.stream()
                .map(Problem::getId)
                .collect(Collectors.toSet());
        Map<Long, List<Submission>> submissionsByUser = submissionRepository
                .findByStatus("ACCEPTED")
                .stream()
                .collect(Collectors.groupingBy(submission -> submission.getUser().getId()));

        List<LeaderboardCandidate> candidates = userRepository.findAll().stream()
                .filter(user -> user.getRole() == Role.USER)
                .map(user -> {
                    List<Submission> allSubmissions = submissionRepository
                            .findByUserIdOrderBySubmittedAtDesc(user.getId());
                    List<Submission> acceptedSubmissions = submissionsByUser
                            .getOrDefault(user.getId(), List.of());
                    int solvedProblems = (int) acceptedSubmissions.stream()
                            .map(submission -> submission.getProblem().getId())
                            .filter(activeProblemIds::contains)
                            .distinct()
                            .count();
                    int submissionCount = allSubmissions.size();
                    int acceptedCount = acceptedSubmissions.size();
                    int acceptanceRate = percentage(acceptedCount, submissionCount);

                    return new LeaderboardCandidate(
                            user, solvedProblems, acceptedCount, submissionCount, acceptanceRate);
                })
                .sorted((left, right) -> {
                    int solvedComparison = Integer.compare(right.solvedProblems, left.solvedProblems);
                    if (solvedComparison != 0) return solvedComparison;
                    int acceptedComparison = Integer.compare(right.acceptedSubmissionCount, left.acceptedSubmissionCount);
                    if (acceptedComparison != 0) return acceptedComparison;
                    return left.user.getUsername().compareToIgnoreCase(right.user.getUsername());
                })
                .toList();

        return java.util.stream.IntStream.range(0, candidates.size())
                .mapToObj(index -> {
                    LeaderboardCandidate candidate = candidates.get(index);
                    return new LeaderboardEntryResponse(
                            index + 1,
                            candidate.user.getId(),
                            candidate.user.getUsername(),
                            candidate.solvedProblems,
                            candidate.acceptedSubmissionCount,
                            candidate.submissionCount,
                            candidate.acceptanceRate);
                })
                .toList();
    }

    private UserProgressResponse buildProgress(
            User user,
            List<Problem> activeProblems,
            List<Submission> submissions) {

        Set<Long> solvedProblemIds = submissions.stream()
                .filter(submission -> "ACCEPTED".equals(submission.getStatus()))
                .map(submission -> submission.getProblem().getId())
                .collect(Collectors.toSet());

        Map<Long, Problem> activeProblemsById = activeProblems.stream()
                .collect(Collectors.toMap(Problem::getId, Function.identity()));

        long easySolved = countSolvedByDifficulty(solvedProblemIds, activeProblemsById, "EASY");
        long mediumSolved = countSolvedByDifficulty(solvedProblemIds, activeProblemsById, "MEDIUM");
        long hardSolved = countSolvedByDifficulty(solvedProblemIds, activeProblemsById, "HARD");
        int acceptedSubmissionCount = (int) submissions.stream()
                .filter(submission -> "ACCEPTED".equals(submission.getStatus()))
                .count();

        List<Long> activeSolvedProblemIds = solvedProblemIds.stream()
                .filter(activeProblemsById::containsKey)
                .sorted()
                .toList();
        int solvedProblems = activeSolvedProblemIds.size();

        return new UserProgressResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                activeProblems.size(),
                solvedProblems,
                (int) easySolved,
                (int) mediumSolved,
                (int) hardSolved,
                percentage(solvedProblems, activeProblems.size()),
                submissions.size(),
                acceptedSubmissionCount,
                percentage(acceptedSubmissionCount, submissions.size()),
                calculateCurrentStreak(submissions),
                activeSolvedProblemIds);
    }

    private long countSolvedByDifficulty(
            Set<Long> solvedProblemIds,
            Map<Long, Problem> activeProblemsById,
            String difficulty) {

        return solvedProblemIds.stream()
                .map(activeProblemsById::get)
                .filter(problem -> problem != null && difficulty.equalsIgnoreCase(problem.getDifficulty()))
                .count();
    }

    private int calculateCurrentStreak(List<Submission> submissions) {

        Set<LocalDate> acceptedDates = submissions.stream()
                .filter(submission -> "ACCEPTED".equals(submission.getStatus()))
                .map(submission -> submission.getSubmittedAt().toLocalDate())
                .collect(Collectors.toSet());

        LocalDate day = LocalDate.now();
        if (!acceptedDates.contains(day)) {
            day = day.minusDays(1);
        }

        int streak = 0;
        while (acceptedDates.contains(day)) {
            streak++;
            day = day.minusDays(1);
        }

        return streak;
    }

    private int percentage(int numerator, int denominator) {
        return denominator == 0 ? 0 : (int) Math.round((numerator * 100.0) / denominator);
    }

    private static class LeaderboardCandidate {
        private final User user;
        private final int solvedProblems;
        private final int acceptedSubmissionCount;
        private final int submissionCount;
        private final int acceptanceRate;

        private LeaderboardCandidate(
                User user,
                int solvedProblems,
                int acceptedSubmissionCount,
                int submissionCount,
                int acceptanceRate) {
            this.user = user;
            this.solvedProblems = solvedProblems;
            this.acceptedSubmissionCount = acceptedSubmissionCount;
            this.submissionCount = submissionCount;
            this.acceptanceRate = acceptanceRate;
        }
    }
}
