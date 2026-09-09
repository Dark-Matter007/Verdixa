package com.leetcode.backend;

import com.leetcode.backend.model.*;
import com.leetcode.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={
        "spring.datasource.url=jdbc:h2:mem:admincontest;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "algosphere.jwt.secret=test-only-secret-that-is-at-least-32-characters"})
@AutoConfigureMockMvc
class AdminContestManagementApiTest {
    @Autowired MockMvc mvc;
    @Autowired ContestRepository contests;
    @Autowired ContestProblemRepository contestProblems;
    @Autowired ContestRegistrationRepository registrations;
    @Autowired SubmissionRepository submissions;
    @Autowired UserRepository users;
    @Autowired ProblemRepository problems;

    User admin, participant;

    @BeforeEach void setUp() {
        admin = users.findByUsername("contest-admin").orElseGet(() -> users.save(new User("contest-admin", "contest-admin@example.test", "hash", Role.ADMIN)));
        participant = users.findByUsername("contest-participant").orElseGet(() -> users.save(new User("contest-participant", "participant@example.test", "hash", Role.USER)));
    }

    @Test @WithMockUser(roles="ADMIN") void adminCanFetchCompactContestList() throws Exception {
        Contest contest = contest("Registry contest", LocalDateTime.now().plusHours(2), LocalDateTime.now().plusHours(4));
        addProblem(contest, problem("Registry problem"), 1);
        register(contest, participant);

        mvc.perform(get("/api/admin/contests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %s)].title".formatted(contest.getId())).value("Registry contest"))
                .andExpect(jsonPath("$[?(@.id == %s)].problemCount".formatted(contest.getId())).value(1))
                .andExpect(jsonPath("$[?(@.id == %s)].registrationCount".formatted(contest.getId())).value(1));
    }

    @Test @WithMockUser(roles="USER") void nonAdminIsDeniedFromEveryAdminContestEndpoint() throws Exception {
        mvc.perform(get("/api/admin/contests")).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/contests/1")).andExpect(status().isForbidden());
        mvc.perform(delete("/api/admin/contests/1")).andExpect(status().isForbidden());
    }

    @Test @WithMockUser(roles="ADMIN") void detailsIncludeProblemRegistrationAndOnlyDistinctInWindowAcceptedProgress() throws Exception {
        LocalDateTime start = LocalDateTime.now().minusHours(1), end = LocalDateTime.now().plusHours(1);
        Contest contest = contest("Progress contest", start, end);
        Problem first = problem("First contest problem"), second = problem("Second contest problem"), outsideProblem = problem("Practice only problem");
        ContestProblem firstLink = addProblem(contest, first, 1), secondLink = addProblem(contest, second, 2);
        register(contest, participant);
        saveSubmission(participant, first, contest, firstLink, "ACCEPTED", start.plusMinutes(10));
        saveSubmission(participant, first, contest, firstLink, "ACCEPTED", start.plusMinutes(20)); // duplicate accepted attempt
        saveSubmission(participant, second, contest, secondLink, "WRONG_ANSWER", start.plusMinutes(30)); // wrong status
        saveSubmission(participant, second, contest, secondLink, "ACCEPTED", end.plusMinutes(1)); // outside window
        saveSubmission(participant, first, null, null, "ACCEPTED", start.plusMinutes(40)); // practice submission to a contest problem
        saveSubmission(participant, outsideProblem, null, null, "ACCEPTED", start.plusMinutes(40)); // non-contest problem

        mvc.perform(get("/api/admin/contests/{id}", contest.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.problemCount").value(2))
                .andExpect(jsonPath("$.problems.length()").value(2))
                .andExpect(jsonPath("$.problems[0].title").value("First contest problem"))
                .andExpect(jsonPath("$.registrations.length()").value(1))
                .andExpect(jsonPath("$.registrations[0].username").value("contest-participant"))
                .andExpect(jsonPath("$.registrations[0].email").value("participant@example.test"))
                .andExpect(jsonPath("$.registrations[0].solvedCount").value(1))
                .andExpect(jsonPath("$.registrations[0].totalProblems").value(2));
    }

    @Test @WithMockUser(roles="ADMIN") void deletionRemovesOnlyContestSpecificLinks() throws Exception {
        Contest contest = contest("Disposable contest", LocalDateTime.now().plusHours(2), LocalDateTime.now().plusHours(3));
        Problem globalProblem = problem("Global problem");
        addProblem(contest, globalProblem, 1);
        register(contest, participant);
        Submission normalSubmission = saveSubmission(participant, globalProblem, null, null, "ACCEPTED", LocalDateTime.now());

        mvc.perform(delete("/api/admin/contests/{id}", contest.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(contest.getId()));

        assertThat(contests.findById(contest.getId())).isEmpty();
        assertThat(contestProblems.findByContestIdOrderByDisplayOrderAsc(contest.getId())).isEmpty();
        assertThat(registrations.findByContestId(contest.getId())).isEmpty();
        assertThat(users.findById(participant.getId())).isPresent();
        assertThat(problems.findById(globalProblem.getId())).isPresent();
        assertThat(submissions.findById(normalSubmission.getId())).isPresent();
    }

    @Test @WithMockUser(roles="ADMIN") void deletionOfNonexistentContestReturnsNotFound() throws Exception {
        mvc.perform(delete("/api/admin/contests/999999")).andExpect(status().isNotFound());
    }

    @Test @WithMockUser(roles="ADMIN") void deletionWithContestSubmissionHistoryReturnsConflictAndPreservesData() throws Exception {
        Contest contest = contest("Historical contest", LocalDateTime.now().minusMinutes(30), LocalDateTime.now().plusMinutes(30));
        Problem globalProblem = problem("Historical global problem");
        ContestProblem link = addProblem(contest, globalProblem, 1);
        register(contest, participant);
        saveSubmission(participant, globalProblem, contest, link, "ACCEPTED", LocalDateTime.now());

        mvc.perform(delete("/api/admin/contests/{id}", contest.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("submission history")));
        assertThat(contests.findById(contest.getId())).isPresent();
        assertThat(submissions.countContestHistoryByContestId(contest.getId())).isEqualTo(1);
    }

    private Contest contest(String title, LocalDateTime start, LocalDateTime end) {
        Contest contest = new Contest();
        contest.setTitle(title); contest.setSlug(title.toLowerCase().replace(' ', '-') + '-' + System.nanoTime());
        contest.setDescription("Test contest"); contest.setStartAt(start); contest.setEndAt(end);
        contest.setStatus(ContestStatus.UPCOMING); contest.setVisibility(ContestVisibility.PUBLIC); contest.setCreatedBy(admin);
        return contests.save(contest);
    }
    private Problem problem(String title) {
        Problem problem = new Problem(); problem.setTitle(title); problem.setDescription("Description"); problem.setDifficulty("EASY"); problem.setActive(true); problem.setTags("Array, Hash Map");
        return problems.save(problem);
    }
    private ContestProblem addProblem(Contest contest, Problem problem, int order) {
        ContestProblem link = new ContestProblem(); link.setContest(contest); link.setProblem(problem); link.setDisplayOrder(order); link.setPoints(100); return contestProblems.save(link);
    }
    private void register(Contest contest, User user) {
        ContestRegistration registration = new ContestRegistration(); registration.setContest(contest); registration.setUser(user); registrations.save(registration);
    }
    private Submission saveSubmission(User user, Problem problem, Contest contest, ContestProblem link, String status, LocalDateTime submittedAt) throws Exception {
        Submission submission = new Submission(); submission.setUser(user); submission.setProblem(problem); submission.setContest(contest); submission.setContestProblem(link); submission.setLanguage("java"); submission.setSourceCode("class Main {}"); submission.setStatus(status); submission.setOutput(""); submission.setErrorMessage(""); submission.setExecutionTimeMs(1L); submission.setPassedTestCases(0); submission.setTotalTestCases(0);
        submission = submissions.save(submission);
        Field field = Submission.class.getDeclaredField("submittedAt"); field.setAccessible(true); field.set(submission, submittedAt);
        return submissions.save(submission);
    }
}
