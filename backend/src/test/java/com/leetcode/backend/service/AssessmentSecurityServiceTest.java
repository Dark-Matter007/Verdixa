package com.leetcode.backend.service;

import com.leetcode.backend.dto.AssessmentRequest;
import com.leetcode.backend.model.*;
import com.leetcode.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AssessmentSecurityServiceTest {
    @Mock AssessmentRepository assessments;
    @Mock AssessmentProblemRepository links;
    @Mock AssessmentInvitationRepository invitations;
    @Mock AssessmentRegistrationRepository registrations;
    @Mock AssessmentSessionRepository sessions;
    @Mock AssessmentProctorEventRepository events;
    @Mock UserRepository users;
    @Mock ProblemRepository problems;
    @Mock SubmissionRepository submissions;
    @Mock AssessmentCreatorService creators;
    @Mock SubmissionService judge;
    @Mock NotificationDeliveryService notifications;

    private AssessmentService service;
    private User owner;
    private User participant;
    private Assessment assessment;

    @BeforeEach
    void setUp() throws Exception {
        service = new AssessmentService(assessments, links, invitations, registrations, sessions,
                events, users, problems, submissions, creators, judge, notifications);
        owner = user("owner", 1L);
        participant = user("participant", 2L);
        assessment = assessment(owner, AssessmentEnums.Visibility.PUBLIC,
                LocalDateTime.now().minusMinutes(5), LocalDateTime.now().plusMinutes(30));
        setId(assessment, 10L);
        when(users.findByUsername("owner")).thenReturn(Optional.of(owner));
        lenient().when(users.findByUsername("participant")).thenReturn(Optional.of(participant));
        lenient().when(assessments.findById(10L)).thenReturn(Optional.of(assessment));
        lenient().when(links.findByAssessmentIdOrderByDisplayOrderAsc(10L)).thenReturn(List.of());
    }

    @Test
    void unapprovedUserCannotCreateAssessment() {
        doThrow(new AccessDeniedException("denied")).when(creators).requireApproved(owner);
        assertThrows(AccessDeniedException.class, () -> service.create("owner", request()));
        verify(assessments, never()).save(any());
    }

    @Test
    void approvedCreatorCanCreateAssessment() {
        when(problems.findById(99L)).thenReturn(Optional.of(mock(Problem.class)));
        service.create("owner", request());
        verify(assessments).save(any(Assessment.class));
        verify(links).save(any(AssessmentProblem.class));
    }

    @Test
    void creatorCannotEditAnotherCreatorsAssessment() {
        User intruder = user("intruder", 3L);
        when(users.findByUsername("intruder")).thenReturn(Optional.of(intruder));
        assertThrows(AccessDeniedException.class, () -> service.update("intruder", 10L, request()));
    }

    @Test
    void revokedCreatorCannotCreateNewAssessment() {
        doThrow(new AccessDeniedException("revoked")).when(creators).requireApproved(owner);
        assertThrows(AccessDeniedException.class, () -> service.create("owner", request()));
    }

    @Test
    void privateAssessmentShowsOnlyMetadataToNonInvitedUser() {
        assessment.setVisibility(AssessmentEnums.Visibility.PRIVATE);
        when(invitations.existsByAssessmentIdAndUserId(10L, 2L)).thenReturn(false);
        assertTrue((Boolean) service.get("participant", 10L).get("accessRestricted"));
        assertThrows(AccessDeniedException.class, () -> service.start("participant", 10L));
    }

    @Test
    void invitedUserStillMustUseEmailAccessGrant() {
        assessment.setVisibility(AssessmentEnums.Visibility.PRIVATE);
        when(invitations.existsByAssessmentIdAndUserId(10L, 2L)).thenReturn(true);
        service.get("participant", 10L);
        assertThrows(AccessDeniedException.class, () -> service.start("participant", 10L));
    }

    @Test
    void publicAssessmentStillRequiresEmailAccessGrant() {
        assertThrows(AccessDeniedException.class, () -> service.start("participant", 10L));
        verify(registrations, never()).save(any(AssessmentRegistration.class));
    }

    @Test
    void duplicatePrivateInvitationsDoNotQueueDuplicateNotification() {
        assessment.setVisibility(AssessmentEnums.Visibility.PRIVATE);
        assessment.setStartAt(LocalDateTime.now().plusHours(1));
        assessment.setEndAt(LocalDateTime.now().plusHours(2));
        when(users.findById(2L)).thenReturn(Optional.of(participant));
        when(invitations.existsByAssessmentIdAndUserId(10L, 2L)).thenReturn(false, true);
        service.invite("owner", 10L, List.of(2L));
        service.invite("owner", 10L, List.of(2L));
        verify(notifications, never()).queueAssessmentInvitations(10L);
        verify(invitations, times(1)).save(any(AssessmentInvitation.class));
    }

    @Test
    void sessionCannotStartOutsideAssessmentWindow() {
        assessment.setStartAt(LocalDateTime.now().plusHours(1));
        assessment.setEndAt(LocalDateTime.now().plusHours(2));
        when(registrations.existsByAssessmentIdAndUserId(10L, 2L)).thenReturn(true);
        assertThrows(AccessDeniedException.class, () -> service.start("participant", 10L));
    }

    @Test
    void assessmentProblemMustBelongToAssessment() throws Exception {
        AssessmentSession session = activeSession();
        setId(session, 20L);
        when(sessions.findById(20L)).thenReturn(Optional.of(session));
        when(links.existsByAssessmentIdAndProblemId(10L, 99L)).thenReturn(false);
        assertThrows(AccessDeniedException.class, () -> service.submit("participant", 20L, 99L, "java", "class Main {}"));
        verify(judge, never()).createSubmission(anyLong(), anyLong(), anyString(), anyString(), any());
    }

    @Test
    void terminatedSessionCannotSubmit() throws Exception {
        AssessmentSession session = activeSession();
        session.setStatus(AssessmentEnums.SessionStatus.TERMINATED);
        setId(session, 20L);
        when(sessions.findById(20L)).thenReturn(Optional.of(session));
        assertThrows(IllegalArgumentException.class, () -> service.submit("participant", 20L, 99L, "java", "class Main {}"));
    }

    @Test
    void fullscreenViolationTerminatesSession() throws Exception {
        assertViolationTerminates(AssessmentEnums.ProctorEventType.FULLSCREEN_EXIT);
    }

    @Test
    void tabVisibilityViolationTerminatesSession() throws Exception {
        assertViolationTerminates(AssessmentEnums.ProctorEventType.TAB_SWITCH);
    }

    @Test
    void microphoneDisabledViolationTerminatesSession() throws Exception {
        assertViolationTerminates(AssessmentEnums.ProctorEventType.MICROPHONE_DISABLED);
    }

    @Test
    void cameraDisabledViolationTerminatesSession() throws Exception {
        assertViolationTerminates(AssessmentEnums.ProctorEventType.CAMERA_DISABLED);
    }

    @Test
    void screenShareStoppedViolationTerminatesSession() throws Exception {
        assertViolationTerminates(AssessmentEnums.ProctorEventType.SCREEN_SHARE_STOPPED);
    }

    @Test
    void windowBlurIsAuditedWithoutTerminatingSession() throws Exception {
        AssessmentSession session = activeSession();
        setId(session, 20L);
        when(sessions.findById(20L)).thenReturn(Optional.of(session));

        service.proctor("participant", 20L, AssessmentEnums.ProctorEventType.WINDOW_BLUR.name(), null);

        assertEquals(AssessmentEnums.SessionStatus.ACTIVE, session.getStatus());
        assertNull(session.getTerminationReason());
        verify(events).save(any(AssessmentProctorEvent.class));
    }

    @Test
    void heartbeatTimeoutTerminatesSession() {
        AssessmentSession session = activeSession();
        session.setLastHeartbeatAt(LocalDateTime.now().minusMinutes(2));
        when(sessions.lockArmedTimedOutSessions(eq(AssessmentEnums.SessionStatus.ACTIVE), any()))
                .thenReturn(List.of(session));
        service.expireHeartbeats(40);
        assertEquals(AssessmentEnums.SessionStatus.TERMINATED, session.getStatus());
        assertEquals("HEARTBEAT_TIMEOUT", session.getTerminationReason());
    }

    @Test
    void authorizedStartInitializesAndRefreshesHeartbeatLease() throws Exception {
        when(sessions.lockByAssessmentIdAndParticipantId(10L, 2L)).thenReturn(Optional.empty());
        when(sessions.save(any(AssessmentSession.class))).thenAnswer(invocation -> {
            AssessmentSession saved = invocation.getArgument(0);
            setId(saved, 20L);
            return saved;
        });

        Map<String,Object> response = service.startAuthorized(participant, 10L);

        assertEquals(AssessmentEnums.SessionStatus.ACTIVE, response.get("status"));
        assertNotNull(response.get("lastHeartbeatAt"));
        verify(events).save(argThat(event -> event.getType()==AssessmentEnums.ProctorEventType.SESSION_STARTED));
    }

    @Test
    void submittedSessionCannotRestartWhenRetriesAreNotAllowed() {
        AssessmentSession session = activeSession();
        session.setStatus(AssessmentEnums.SessionStatus.SUBMITTED);
        when(sessions.lockByAssessmentIdAndParticipantId(10L, 2L)).thenReturn(Optional.of(session));

        assertThrows(IllegalArgumentException.class, () -> service.startAuthorized(participant, 10L));
        verify(events, never()).save(argThat(event -> event.getType()==AssessmentEnums.ProctorEventType.SESSION_STARTED));
    }

    @Test
    void unarmedStartupEventCannotTerminateActiveSession() throws Exception {
        AssessmentSession session = activeSession();
        session.setProctoringArmedAt(null);
        setId(session, 20L);
        when(sessions.findById(20L)).thenReturn(Optional.of(session));

        service.proctor("participant", 20L, AssessmentEnums.ProctorEventType.FULLSCREEN_EXIT.name(), null);

        assertEquals(AssessmentEnums.SessionStatus.ACTIVE, session.getStatus());
        assertNull(session.getTerminationReason());
        verify(events, never()).save(argThat(event -> event.getType()==AssessmentEnums.ProctorEventType.FULLSCREEN_EXIT));
    }

    @Test
    void immediatePostStartSessionIsNotSelectedForHeartbeatTimeout() throws Exception {
        AssessmentSession session = activeSession();
        session.setProctoringArmedAt(null);
        setId(session, 20L);
        when(sessions.lockArmedTimedOutSessions(eq(AssessmentEnums.SessionStatus.ACTIVE), any())).thenReturn(List.of());

        service.expireHeartbeats(40);

        assertEquals(AssessmentEnums.SessionStatus.ACTIVE, session.getStatus());
        verify(events, never()).save(argThat(event -> event.getType()==AssessmentEnums.ProctorEventType.HEARTBEAT_TIMEOUT));
    }

    @Test
    void duplicateTerminationIsIdempotentAndFirstReasonWins() throws Exception {
        AssessmentSession session = activeSession();
        setId(session, 20L);
        when(sessions.findById(20L)).thenReturn(Optional.of(session));

        service.proctor("participant", 20L, AssessmentEnums.ProctorEventType.TAB_SWITCH.name(), null);
        service.proctor("participant", 20L, AssessmentEnums.ProctorEventType.FULLSCREEN_EXIT.name(), null);

        assertEquals(AssessmentEnums.SessionStatus.TERMINATED, session.getStatus());
        assertEquals("TAB_SWITCH", session.getTerminationReason());
        verify(events, times(1)).save(any(AssessmentProctorEvent.class));
    }

    @Test
    void heartbeatTimeoutRequiresConfiguredCutoffSelection() {
        when(sessions.lockArmedTimedOutSessions(eq(AssessmentEnums.SessionStatus.ACTIVE), any())).thenReturn(List.of());

        service.expireHeartbeats(40);

        verify(sessions).lockArmedTimedOutSessions(eq(AssessmentEnums.SessionStatus.ACTIVE), argThat(cutoff -> cutoff.isBefore(LocalDateTime.now().minusSeconds(35))));
        verify(events, never()).save(any());
    }

    @Test
    void userCannotAccessAnotherCreatorsAnalytics() {
        assertThrows(AccessDeniedException.class, () -> service.analytics("participant", 10L, false));
    }

    @Test
    void publicUnregisteredUserCannotStart() {
        assertThrows(AccessDeniedException.class, () -> service.start("participant", 10L));
    }

    @Test
    void legacyRegistrationCannotStartWhileLive() {
        when(registrations.existsByAssessmentIdAndUserId(10L, 2L)).thenReturn(true);
        when(sessions.findByAssessmentIdAndParticipantId(10L, 2L)).thenReturn(Optional.empty());
        when(sessions.save(any(AssessmentSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        assertThrows(AccessDeniedException.class, () -> service.start("participant", 10L));
    }

    @Test
    void privateInvitedButUnregisteredUserCannotStart() {
        assessment.setVisibility(AssessmentEnums.Visibility.PRIVATE);
        when(invitations.existsByAssessmentIdAndUserId(10L, 2L)).thenReturn(true);
        assertThrows(AccessDeniedException.class, () -> service.start("participant", 10L));
    }

    @Test
    void privateLegacyRegistrationCannotBypassEmailAccess() {
        assessment.setVisibility(AssessmentEnums.Visibility.PRIVATE);
        when(invitations.existsByAssessmentIdAndUserId(10L, 2L)).thenReturn(true);
        when(registrations.existsByAssessmentIdAndUserId(10L, 2L)).thenReturn(true);
        when(sessions.findByAssessmentIdAndParticipantId(10L, 2L)).thenReturn(Optional.empty());
        when(sessions.save(any(AssessmentSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        assertThrows(AccessDeniedException.class, () -> service.start("participant", 10L));
    }

    @Test
    void safeDraftDeletionRemovesOnlyDraftRecords() {
        assessment.setStatus(AssessmentEnums.Status.DRAFT);
        when(sessions.findByAssessmentId(10L)).thenReturn(List.of());
        service.cancel("owner", 10L);
        verify(links).deleteByAssessmentId(10L);
        verify(invitations).deleteByAssessmentId(10L);
        verify(assessments).delete(assessment);
    }

    @Test
    void cancelledAssessmentCannotStart() {
        assessment.setStatus(AssessmentEnums.Status.CANCELLED);
        assertThrows(AccessDeniedException.class, () -> service.start("participant", 10L));
    }

    @Test
    void creatorCanEditOwnDraft() {
        assessment.setStatus(AssessmentEnums.Status.DRAFT);
        when(problems.findById(99L)).thenReturn(Optional.of(mock(Problem.class)));
        assertNotNull(service.update("owner", 10L, request()));
        verify(links).deleteByAssessmentId(10L);
    }

    @Test
    void startedInviteeCannotBeSilentlyRemoved() {
        assessment.setVisibility(AssessmentEnums.Visibility.PRIVATE);
        when(sessions.findByAssessmentIdAndParticipantId(10L, 2L)).thenReturn(Optional.of(activeSession()));
        assertThrows(IllegalArgumentException.class, () -> service.removeInvitee("owner", 10L, 2L));
    }

    private void assertViolationTerminates(AssessmentEnums.ProctorEventType type) throws Exception {
        AssessmentSession session = activeSession();
        setId(session, 20L);
        when(sessions.findById(20L)).thenReturn(Optional.of(session));
        service.proctor("participant", 20L, type.name(), null);
        assertEquals(AssessmentEnums.SessionStatus.TERMINATED, session.getStatus());
        assertEquals(type.name(), session.getTerminationReason());
    }

    private AssessmentSession activeSession() {
        AssessmentSession session = new AssessmentSession();
        session.setAssessment(assessment);
        session.setParticipant(participant);
        session.setStatus(AssessmentEnums.SessionStatus.ACTIVE);
        session.setLastHeartbeatAt(LocalDateTime.now());
        session.setProctoringArmedAt(LocalDateTime.now());
        try {
            Field startedAt = AssessmentSession.class.getDeclaredField("startedAt");
            startedAt.setAccessible(true);
            startedAt.set(session, LocalDateTime.now());
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
        return session;
    }

    private AssessmentRequest request() {
        return new AssessmentRequest("Hiring screen", "Verdixa", "Description", "Instructions",
                AssessmentEnums.Visibility.PUBLIC, LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusHours(2), LocalDateTime.now().plusMinutes(30), 50,
                true, true, true, List.of(new AssessmentRequest.ProblemSelection(99L, 100)),
                List.of(), List.of());
    }

    private static User user(String username, Long id) {
        User user = mock(User.class);
        when(user.getUsername()).thenReturn(username);
        when(user.getId()).thenReturn(id);
        when(user.getRole()).thenReturn(Role.USER);
        when(user.isEmailVerified()).thenReturn(true);
        return user;
    }

    private static Assessment assessment(User creator, AssessmentEnums.Visibility visibility,
                                         LocalDateTime start, LocalDateTime end) {
        Assessment assessment = new Assessment();
        assessment.setCreator(creator);
        assessment.setTitle("Assessment");
        assessment.setOrganization("Verdixa");
        assessment.setDescription("Description");
        assessment.setInstructions("Instructions");
        assessment.setVisibility(visibility);
        assessment.setStatus(AssessmentEnums.Status.PUBLISHED);
        assessment.setStartAt(start);
        assessment.setEndAt(end);
        assessment.setInviteToken("token");
        return assessment;
    }

    private static void setId(Object target, Long id) throws Exception {
        Field field = target.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(target, id);
    }
}
