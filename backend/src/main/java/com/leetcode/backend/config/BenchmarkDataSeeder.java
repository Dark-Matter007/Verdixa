package com.leetcode.backend.config;

import com.leetcode.backend.model.ExecutionMode;
import com.leetcode.backend.model.Problem;
import com.leetcode.backend.model.Role;
import com.leetcode.backend.model.Submission;
import com.leetcode.backend.model.User;
import com.leetcode.backend.repository.ProblemRepository;
import com.leetcode.backend.repository.SubmissionRepository;
import com.leetcode.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;

/** Deterministic, local-only data for the HTTP benchmark profile. */
@Configuration
@Profile("benchmark")
public class BenchmarkDataSeeder {

    @Bean
    CommandLineRunner benchmarkSeedData(
            UserRepository users,
            ProblemRepository problems,
            SubmissionRepository submissions,
            PasswordEncoder passwordEncoder,
            @Value("${algosphere.benchmark.seed:false}") boolean enabled) {
        return args -> {
            if (!enabled || users.existsByUsername("benchmark-user")) {
                return;
            }
            User user = new User("benchmark-user", "benchmark-user@local.test",
                    passwordEncoder.encode("benchmark-password"), Role.USER);
            user = users.save(user);

            List<Problem> seededProblems = new ArrayList<>();
            for (int index = 1; index <= 100; index++) {
                Problem problem = new Problem();
                problem.setTitle(String.format("Benchmark problem %03d", index));
                problem.setDescription("Deterministic benchmark record " + index);
                problem.setDifficulty(index % 3 == 0 ? "HARD" : index % 2 == 0 ? "MEDIUM" : "EASY");
                problem.setTags(index % 2 == 0 ? "array,benchmark" : "string,benchmark");
                problem.setExecutionMode(ExecutionMode.STDIN);
                problem.setActive(true);
                seededProblems.add(problem);
            }
            seededProblems = problems.saveAll(seededProblems);

            List<Submission> seededSubmissions = new ArrayList<>();
            for (Problem problem : seededProblems) {
                for (int attempt = 0; attempt < 10; attempt++) {
                    Submission submission = new Submission();
                    submission.setUser(user);
                    submission.setProblem(problem);
                    submission.setSourceCode("// benchmark submission");
                    submission.setLanguage("JAVA");
                    submission.setStatus(attempt % 2 == 0 ? "ACCEPTED" : "WRONG_ANSWER");
                    submission.setOutput("");
                    submission.setPassedTestCases(attempt % 2 == 0 ? 4 : 0);
                    submission.setTotalTestCases(4);
                    seededSubmissions.add(submission);
                }
            }
            submissions.saveAll(seededSubmissions);
            System.out.printf("Benchmark seed complete: users=1, problems=%d, submissions=%d%n",
                    seededProblems.size(), seededSubmissions.size());
        };
    }
}
