package com.leetcode.backend.service;

import com.leetcode.backend.model.*;
import com.leetcode.backend.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotificationIdempotencyTest {
    @Test
    void contestAnnouncementIsQueuedOnlyOnce() throws Exception {
        Fixture fixture = new Fixture();
        when(fixture.users.findByEmailVerifiedTrue()).thenReturn(List.of(fixture.user));
        when(fixture.contests.findById(10L)).thenReturn(Optional.of(fixture.contest));
        when(fixture.deliveries.existsByUserIdAndTypeAndEntityTypeAndEntityId(
                1L, "CONTEST_ANNOUNCEMENT", "CONTEST", 10L)).thenReturn(false, true);

        fixture.service.queueContestAnnouncement(10L);
        fixture.service.queueContestAnnouncement(10L);

        verify(fixture.deliveries, times(1)).save(any(NotificationDelivery.class));
    }

    @Test
    void tenMinuteReminderIsQueuedOnlyOnceAcrossSchedulerRuns() throws Exception {
        Fixture fixture = new Fixture();
        fixture.contest.setStartAt(LocalDateTime.now().plusMinutes(10));
        when(fixture.contests.findAll()).thenReturn(List.of(fixture.contest));
        when(fixture.registrations.findWithUserByContestIdOrderByRegisteredAtAsc(10L))
                .thenReturn(List.of(registration(fixture.contest, fixture.user)));
        when(fixture.deliveries.existsByUserIdAndTypeAndEntityTypeAndEntityId(
                1L, "CONTEST_TEN_MINUTE_REMINDER", "CONTEST", 10L)).thenReturn(false, true);

        fixture.service.scheduleAndSend();
        fixture.service.scheduleAndSend();

        verify(fixture.deliveries, times(1)).save(any(NotificationDelivery.class));
    }

    @Test
    void liveEmailIsQueuedOnlyOnceAcrossSchedulerRuns() throws Exception {
        Fixture fixture = new Fixture();
        fixture.contest.setStartAt(LocalDateTime.now().minusSeconds(20));
        when(fixture.contests.findAll()).thenReturn(List.of(fixture.contest));
        when(fixture.registrations.findWithUserByContestIdOrderByRegisteredAtAsc(10L))
                .thenReturn(List.of(registration(fixture.contest, fixture.user)));
        when(fixture.deliveries.existsByUserIdAndTypeAndEntityTypeAndEntityId(
                1L, "CONTEST_LIVE", "CONTEST", 10L)).thenReturn(false, true);

        fixture.service.scheduleAndSend();
        fixture.service.scheduleAndSend();

        verify(fixture.deliveries, times(1)).save(any(NotificationDelivery.class));
    }

    @Test
    void creatorApprovalPublishesOneDecisionEmailEvent() {
        User admin = mock(User.class);
        User applicant = mock(User.class);
        when(admin.getUsername()).thenReturn("admin");
        when(applicant.getUsername()).thenReturn("creator");
        when(applicant.getEmail()).thenReturn("creator@example.test");
        AssessmentCreatorApplication application = new AssessmentCreatorApplication();
        application.setUser(applicant);
        application.setStatus(AssessmentEnums.CreatorStatus.PENDING_ADMIN_REVIEW);
        application.setFullName("Creator");
        application.setOrganization("Verdixa");
        application.setRoleTitle("Engineer");
        application.setPurpose("Hiring");
        application.setIntendedUse("Assess candidates");
        application.setCountry("IN");
        application.setDocumentType("Passport");

        UserRepository users = mock(UserRepository.class);
        AssessmentCreatorApplicationRepository applications = mock(AssessmentCreatorApplicationRepository.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        when(users.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(applications.findById(30L)).thenReturn(Optional.of(application));
        AssessmentCreatorService service = new AssessmentCreatorService(users, applications,
                mock(AssessmentCreatorDocumentRepository.class), mock(AssessmentCreatorReviewAuditRepository.class),
                mock(VerificationDocumentStorage.class), mock(PasswordEncoder.class), publisher);

        service.decide("admin", 30L, AssessmentEnums.CreatorStatus.APPROVED, null);
        assertThrows(IllegalArgumentException.class,
                () -> service.decide("admin", 30L, AssessmentEnums.CreatorStatus.APPROVED, null));

        verify(publisher, times(1)).publishEvent(any(EmailNotificationEvents.CreatorDecision.class));
    }

    @Test
    void publicAssessmentAnnouncementIsQueuedOnlyOnce() throws Exception {
        Fixture fixture = new Fixture();
        fixture.assessment.setVisibility(AssessmentEnums.Visibility.PUBLIC);
        when(fixture.users.findByEmailVerifiedTrue()).thenReturn(List.of(fixture.user));
        when(fixture.assessments.findById(20L)).thenReturn(Optional.of(fixture.assessment));
        when(fixture.deliveries.existsByUserIdAndTypeAndEntityTypeAndEntityId(1L, "ASSESSMENT_PUBLIC_ANNOUNCEMENT", "ASSESSMENT", 20L)).thenReturn(false, true);
        fixture.assessmentService.queueAssessmentPublished(20L);
        fixture.assessmentService.queueAssessmentPublished(20L);
        verify(fixture.deliveries, times(1)).save(any(NotificationDelivery.class));
    }

    @Test
    void assessmentRegistrationConfirmationIsQueuedOnlyOnce() throws Exception {
        Fixture fixture = new Fixture();
        when(fixture.assessmentRegistrations.findByAssessmentId(20L)).thenReturn(List.of(assessmentRegistration(fixture.assessment, fixture.user)));
        when(fixture.deliveries.existsByUserIdAndTypeAndEntityTypeAndEntityId(1L, "ASSESSMENT_REGISTRATION_CONFIRMATION", "ASSESSMENT", 20L)).thenReturn(false, true);
        fixture.assessmentService.queueAssessmentRegistration(20L, 1L);
        fixture.assessmentService.queueAssessmentRegistration(20L, 1L);
        verify(fixture.deliveries, times(1)).save(any(NotificationDelivery.class));
    }

    private static ContestRegistration registration(Contest contest, User user) {
        ContestRegistration registration = new ContestRegistration();
        registration.setContest(contest);
        registration.setUser(user);
        return registration;
    }

    private static AssessmentRegistration assessmentRegistration(Assessment assessment, User user) {
        AssessmentRegistration registration = new AssessmentRegistration();
        registration.setAssessment(assessment);
        registration.setUser(user);
        return registration;
    }

    private static final class Fixture {
        final NotificationDeliveryRepository deliveries = mock(NotificationDeliveryRepository.class);
        final UserRepository users = mock(UserRepository.class);
        final ContestRepository contests = mock(ContestRepository.class);
        final ContestRegistrationRepository registrations = mock(ContestRegistrationRepository.class);
        final AssessmentRepository assessments = mock(AssessmentRepository.class);
        final AssessmentRegistrationRepository assessmentRegistrations = mock(AssessmentRegistrationRepository.class);
        final AssessmentInvitationRepository invitations = mock(AssessmentInvitationRepository.class);
        final AssessmentProblemRepository assessmentProblems = mock(AssessmentProblemRepository.class);
        final EmailService email = mock(EmailService.class);
        final User user = mock(User.class);
        final Contest contest = new Contest();
        final Assessment assessment = new Assessment();
        final NotificationDeliveryService service;
        final NotificationDeliveryService assessmentService;

        Fixture() throws Exception {
            when(user.getId()).thenReturn(1L);
            when(user.getEmail()).thenReturn("user@example.test");
            when(user.getUsername()).thenReturn("user");
            when(user.isEmailVerified()).thenReturn(true);
            contest.setTitle("Contest");
            contest.setStatus(ContestStatus.UPCOMING);
            contest.setStartAt(LocalDateTime.now().plusHours(1));
            contest.setEndAt(LocalDateTime.now().plusHours(2));
            Field id = Contest.class.getDeclaredField("id");
            id.setAccessible(true);
            id.set(contest, 10L);
            assessment.setCreator(user);
            assessment.setTitle("Assessment");
            assessment.setOrganization("Verdixa");
            assessment.setVisibility(AssessmentEnums.Visibility.PUBLIC);
            assessment.setStatus(AssessmentEnums.Status.PUBLISHED);
            assessment.setStartAt(LocalDateTime.now().plusHours(1));
            assessment.setEndAt(LocalDateTime.now().plusHours(2));
            Field assessmentId = Assessment.class.getDeclaredField("id");
            assessmentId.setAccessible(true);
            assessmentId.set(assessment, 20L);
            when(deliveries.findTop100ByStatusOrderByIdAsc(AssessmentEnums.DeliveryStatus.PENDING)).thenReturn(List.of());
            service = new NotificationDeliveryService(deliveries, users, contests, registrations,
                    assessments, email, 10);
            assessmentService = new NotificationDeliveryService(deliveries, users, contests, registrations,
                    assessments, assessmentRegistrations, invitations, assessmentProblems, email, 10, 10);
        }
    }
}
