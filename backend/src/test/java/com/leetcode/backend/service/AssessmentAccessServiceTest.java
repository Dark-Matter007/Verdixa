package com.leetcode.backend.service;

import com.leetcode.backend.dto.AssessmentAccessRequest;
import com.leetcode.backend.model.*;
import com.leetcode.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AssessmentAccessServiceTest {
    @Mock AssessmentRepository assessments; @Mock AssessmentAccessRepository accesses;
    @Mock AssessmentInvitationRepository invitations; @Mock UserRepository users;
    @Mock AssessmentProblemRepository problems; @Mock AssessmentSessionRepository sessions;
    @Mock EmailService email; @Mock AssessmentService assessmentService;
    private AssessmentAccessService service; private Assessment assessment; private AssessmentAccess access; private BCryptPasswordEncoder encoder;

    @BeforeEach void setUp() throws Exception {
        encoder=new BCryptPasswordEncoder(); service=new AssessmentAccessService(assessments,accesses,invitations,users,problems,sessions,encoder,email,assessmentService);
        assessment=new Assessment(); assessment.setCreator(new User("host","host@example.com","x",Role.USER)); assessment.setStatus(AssessmentEnums.Status.PUBLISHED); assessment.setVisibility(AssessmentEnums.Visibility.PUBLIC); assessment.setTitle("Screen"); assessment.setOrganization("Verdixa"); assessment.setStartAt(LocalDateTime.now().plusMinutes(20)); assessment.setEndAt(LocalDateTime.now().plusHours(2)); setId(assessment,7L);
        access=new AssessmentAccess(); setId(access,11L); when(assessments.findPreflightById(7L)).thenReturn(Optional.of(assessment)); when(invitations.findForAccessByAssessmentId(7L)).thenReturn(List.of()); when(accesses.findByAssessmentIdAndEmailIgnoreCase(7L,"candidate@example.com")).thenReturn(Optional.of(access)); when(accesses.countByAssessmentIdAndVerifiedAtIsNotNull(7L)).thenReturn(0L); when(accesses.save(any())).thenAnswer(i->i.getArgument(0));
    }

    @Test void otpIsHashedAndSingleUse() throws Exception {
        ArgumentCaptor<String> code=ArgumentCaptor.forClass(String.class);
        service.request(7L,request(),"127.0.0.1");
        verify(email).sendAssessmentAccessOtp(eq("candidate@example.com"),anyString(),eq("Screen"),code.capture());
        assertNotEquals(code.getValue(),access.getOtpHash());
        assertTrue(access.getOtpHash().startsWith("$2"));
        when(accesses.findByIdAndAssessmentId(11L,7L)).thenReturn(Optional.of(access));
        User participant=new User("candidate","candidate@example.com","x",Role.USER); setId(participant,33L); when(users.findByEmailIgnoreCase("candidate@example.com")).thenReturn(Optional.of(participant));
        assertNotNull(service.verify(7L,11L,code.getValue()).get("grant"));
        assertNull(access.getOtpHash());
        assertThrows(IllegalArgumentException.class,()->service.verify(7L,11L,code.getValue()));
    }

    @Test void wrongCodeIsRejectedAndCounted() {
        service.request(7L,request(),"127.0.0.1"); when(accesses.findByIdAndAssessmentId(11L,7L)).thenReturn(Optional.of(access));
        assertThrows(IllegalArgumentException.class,()->service.verify(7L,11L,"000000"));
        assertEquals(1,access.getFailedAttempts());
    }

    @Test void expiredCodeIsRejected() {
        service.request(7L,request(),"127.0.0.1"); access.setOtpExpiresAt(LocalDateTime.now().minusSeconds(1)); when(accesses.findByIdAndAssessmentId(11L,7L)).thenReturn(Optional.of(access));
        assertThrows(IllegalArgumentException.class,()->service.verify(7L,11L,"123456"));
    }

    @Test void privateAssessmentRejectsMissingInvitationBeforeMail() {
        assessment.setVisibility(AssessmentEnums.Visibility.PRIVATE);
        assertThrows(AccessDeniedException.class,()->service.request(7L,request(),"127.0.0.1"));
        verifyNoInteractions(email);
    }

    @Test void overviewExposesRequirementsButKeepsQuestionsLocked() {
        assessment.setStrictProctoring(true); assessment.setFullscreenRequired(true); assessment.setMicrophoneRequired(true);
        var result=service.overview(7L,null);
        assertTrue(result.strictProctoring());
        assertEquals(0,result.questionCount());
        assertNull(result.problems());
        assertNull(result.mcqs());
    }

    @Test void liveStartReturnsOnlyProblemsSelectedForThisAssessment() throws Exception {
        assessment.setStartAt(LocalDateTime.now().minusMinutes(1));
        User participant=new User("candidate","candidate@example.com","x",Role.ASSESSMENT_GUEST); setId(participant,33L);
        access.setAssessment(assessment); access.setParticipant(participant); access.setGrantHash(encoder.encode("verified-grant")); access.setGrantExpiresAt(assessment.getEndAt());
        Problem selected=new Problem(); setId(selected,99L); selected.setTitle("Assigned arrays"); selected.setDescription("Solve only this assigned problem."); selected.setDifficulty("MEDIUM"); selected.setStarterCode("{\"java\":\"class Main {}\"}");
        AssessmentProblem selection=new AssessmentProblem(); selection.setAssessment(assessment); selection.setProblem(selected); selection.setDisplayOrder(1); selection.setPoints(100);
        when(accesses.findAllForGrantValidation()).thenReturn(List.of(access)); when(problems.findByAssessmentIdOrderByDisplayOrderAsc(7L)).thenReturn(List.of(selection));
        when(assessmentService.startAuthorized(participant,7L)).thenReturn(java.util.Map.of("id",22L,"endsAt",assessment.getEndAt()));
        var result=service.start(7L,"verified-grant");
        var secureAssessment=(com.leetcode.backend.dto.AssessmentPreflightResponse)result.get("assessment");
        var assigned=secureAssessment.problems();
        assertEquals(1,assigned.size());
        assertEquals(99L,assigned.get(0).get("id"));
        assertEquals("Solve only this assigned problem.",assigned.get(0).get("description"));
        verify(assessmentService).startAuthorized(participant,7L);
    }

    private static AssessmentAccessRequest request(){return new AssessmentAccessRequest("Candidate","candidate@example.com",null,null,"confirmed",null);}
    private static void setId(Object target,Long id) throws Exception {Field field=target.getClass().getDeclaredField("id");field.setAccessible(true);field.set(target,id);}
}
