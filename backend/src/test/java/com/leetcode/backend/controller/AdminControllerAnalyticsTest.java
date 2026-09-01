package com.leetcode.backend.controller;

import com.leetcode.backend.model.Submission;
import com.leetcode.backend.model.User;
import com.leetcode.backend.repository.ProblemRepository;
import com.leetcode.backend.repository.SubmissionRepository;
import com.leetcode.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class AdminControllerAnalyticsTest {
    @Test void metricsUseDistinctUsersAndInclusiveCalendarWindows() {
        var users=mock(UserRepository.class); var problems=mock(ProblemRepository.class); var submissions=mock(SubmissionRepository.class);
        User first=user(1L), second=user(2L);
        Submission today=submission(first,"ACCEPTED",LocalDate.now().atTime(12,0));
        Submission boundary=submission(first,"WRONG_ANSWER",LocalDate.now().minusDays(29).atStartOfDay());
        Submission outside=submission(second,"ACCEPTED",LocalDate.now().minusDays(30).atTime(23,59));
        when(submissions.findAll()).thenReturn(List.of(today,boundary,outside)); when(users.count()).thenReturn(7L); when(problems.countByActiveTrue()).thenReturn(4L);

        var body=new AdminController(users,problems,submissions).analytics().getBody();
        assertEquals(7L,body.get("totalUsers")); assertEquals(4L,body.get("publishedProblems"));
        assertEquals(1L,body.get("activeUsers")); assertEquals(3,body.get("totalSubmissions"));
        assertEquals(2L,body.get("acceptedSubmissions")); assertEquals(67L,body.get("acceptanceRate"));
        assertEquals(1L,body.get("submissionsToday")); assertEquals(1L,body.get("submissionsLast7Days")); assertEquals(2L,body.get("submissionsLast30Days"));
    }

    private User user(long id){User user=mock(User.class);when(user.getId()).thenReturn(id);return user;}
    private Submission submission(User user,String status,LocalDateTime at){Submission value=mock(Submission.class);when(value.getUser()).thenReturn(user);when(value.getStatus()).thenReturn(status);when(value.getLanguage()).thenReturn("java");when(value.getSubmittedAt()).thenReturn(at);return value;}
}
