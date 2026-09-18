package com.leetcode.backend.service;

import com.leetcode.backend.dto.AssessmentAccessRequest;
import com.leetcode.backend.dto.AssessmentParticipantIdentity;
import com.leetcode.backend.dto.AssessmentPreflightResponse;
import com.leetcode.backend.model.*;
import com.leetcode.backend.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Owns the anonymous assessment flow. A grant is deliberately not an application login. */
@Service
public class AssessmentAccessService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MAX_ATTEMPTS = 5;
    private final ConcurrentHashMap<String,Deque<LocalDateTime>> ipIssues = new ConcurrentHashMap<>();
    private final AssessmentRepository assessments;
    private final AssessmentAccessRepository accesses;
    private final AssessmentAccessAuditRepository audits;
    private final AssessmentInvitationRepository invitations;
    private final UserRepository users;
    private final AssessmentProblemRepository problems;
    private final AssessmentSessionRepository sessions;
    private final PasswordEncoder encoder;
    private final EmailService email;
    private final AssessmentService assessmentService;

    @org.springframework.beans.factory.annotation.Autowired public AssessmentAccessService(AssessmentRepository assessments, AssessmentAccessRepository accesses, AssessmentAccessAuditRepository audits,
            AssessmentInvitationRepository invitations, UserRepository users, AssessmentProblemRepository problems, AssessmentSessionRepository sessions, PasswordEncoder encoder,
            EmailService email, AssessmentService assessmentService) {
        this.assessments=assessments; this.accesses=accesses; this.audits=audits; this.invitations=invitations; this.users=users;
        this.problems=problems; this.sessions=sessions; this.encoder=encoder; this.email=email; this.assessmentService=assessmentService;
    }
    /** Compatibility constructor for focused tests and integrations prior to access auditing. */
    public AssessmentAccessService(AssessmentRepository assessments, AssessmentAccessRepository accesses,
            AssessmentInvitationRepository invitations, UserRepository users, AssessmentProblemRepository problems, AssessmentSessionRepository sessions, PasswordEncoder encoder,
            EmailService email, AssessmentService assessmentService) { this(assessments,accesses,null,invitations,users,problems,sessions,encoder,email,assessmentService); }

    @Transactional(readOnly=true)
    public AssessmentPreflightResponse overview(Long assessmentId, String invite) {
        Assessment a = assessment(assessmentId); AssessmentInvitation invitation = invitation(a, invite);
        if (a.getVisibility()==AssessmentEnums.Visibility.PRIVATE && invitation==null) throw new AccessDeniedException("This assessment invitation is invalid or unavailable.");
        String registrationStatus=invitation==null?"ACCESS_REQUIRED":"INVITED";
        return summary(a, invitation, false, registrationStatus, true);
    }

    @Transactional
    public Map<String,Object> request(Long assessmentId, AssessmentAccessRequest request, String clientIp) {
        Assessment a=assessment(assessmentId); assertAvailable(a);
        enforceIpIssueLimit(assessmentId, clientIp);
        AssessmentInvitation invitation=invitation(a,request.invite());
        if(a.getVisibility()==AssessmentEnums.Visibility.PRIVATE) {
            if(invitation==null || invitation.getStatus()==AssessmentEnums.InvitationStatus.CANCELLED) throw new AccessDeniedException("This assessment invitation is invalid or unavailable.");
        }
        String emailAddress=a.getVisibility()==AssessmentEnums.Visibility.PRIVATE?invitation.getUser().getEmail():normalEmail(request.email());
        LocalDateTime now=LocalDateTime.now();
        AssessmentAccess access=accesses.findByAssessmentIdAndEmailIgnoreCase(assessmentId,emailAddress).orElseGet(AssessmentAccess::new);
        if(access.getId()==null && a.getMaxParticipants()!=null && accesses.countByAssessmentIdAndVerifiedAtIsNotNull(assessmentId)>=a.getMaxParticipants()) throw new IllegalArgumentException("This assessment is currently at capacity.");
        if(access.getResendAvailableAt()!=null && now.isBefore(access.getResendAvailableAt())) throw new IllegalArgumentException("Please wait before requesting another code.");
        access.setAssessment(a); access.setInvitation(invitation); access.setEmail(emailAddress); access.setFullName(clean(request.fullName()));
        access.setParticipantReference(clean(request.participantId())); access.setOrganization(clean(request.organization())); String otp=issue(access,now);
        accesses.save(access); audit(access,"OTP_REQUESTED",clientIp); email.sendAssessmentAccessOtp(emailAddress, access.getFullName(), a.getTitle(), otp);
        return Map.of("accessId",access.getId(),"maskedEmail",mask(emailAddress),"expiresAt",access.getOtpExpiresAt(),"resendAvailableAt",access.getResendAvailableAt());
    }
    public Map<String,Object> request(Long assessmentId, AssessmentAccessRequest request) { return request(assessmentId,request,"unknown"); }

    /** The clear OTP is returned only to the caller for immediate mail delivery; it is never persisted. */
    private String issue(AssessmentAccess access, LocalDateTime now) {
        if(access.getIssueWindowStartedAt()==null || access.getIssueWindowStartedAt().isBefore(now.minusHours(1))){access.setIssueWindowStartedAt(now);access.setIssueCount(0);} if(access.getIssueCount()>=5)throw new IllegalArgumentException("Too many code requests. Try again later."); access.setIssueCount(access.getIssueCount()+1);
        String otp=String.format("%06d", RANDOM.nextInt(1_000_000)); access.setOtpHash(encoder.encode(otp));
        access.setOtpExpiresAt(now.plusMinutes(10)); access.setResendAvailableAt(now.plusSeconds(60)); access.setFailedAttempts(0);
        access.setVerifiedAt(null); access.setGrantHash(null); access.setGrantExpiresAt(null); return otp;
    }

    @Transactional
    public Map<String,Object> resend(Long assessmentId, Long accessId) {
        AssessmentAccess access=access(accessId,assessmentId); assertAvailable(access.getAssessment());
        if(LocalDateTime.now().isBefore(access.getResendAvailableAt())) throw new IllegalArgumentException("Please wait before requesting another code.");
        String otp=issue(access,LocalDateTime.now()); accesses.save(access); audit(access,"OTP_RESENT",null); email.sendAssessmentAccessOtp(access.getEmail(),access.getFullName(),access.getAssessment().getTitle(),otp);
        return Map.of("maskedEmail",mask(access.getEmail()),"expiresAt",access.getOtpExpiresAt(),"resendAvailableAt",access.getResendAvailableAt());
    }

    @Transactional
    public Map<String,Object> verify(Long assessmentId, Long accessId, String otp) {
        AssessmentAccess access=access(accessId,assessmentId); assertAvailable(access.getAssessment()); LocalDateTime now=LocalDateTime.now();
        if(access.getOtpHash()==null || access.getOtpExpiresAt()==null || now.isAfter(access.getOtpExpiresAt())) throw new IllegalArgumentException("That code has expired. Request a new code.");
        if(access.getFailedAttempts()>=MAX_ATTEMPTS) throw new IllegalArgumentException("Too many attempts. Request a new code.");
        if(!encoder.matches(otp,access.getOtpHash())) { access.setFailedAttempts(access.getFailedAttempts()+1); if(access.getFailedAttempts()>=MAX_ATTEMPTS) access.setOtpHash(null); audit(access,"OTP_FAILED",null); throw new IllegalArgumentException(access.getFailedAttempts()>=MAX_ATTEMPTS?"Too many attempts. Request a new code.":"That code is incorrect."); }
        String grant=randomToken(); access.setOtpHash(null); access.setOtpExpiresAt(now); access.setVerifiedAt(now); access.setGrantHash(encoder.encode(grant)); access.setGrantExpiresAt(access.getAssessment().getEndAt());
        User participant=resolveParticipant(access); participant.setEmailVerified(true); access.setParticipant(participant); accesses.save(access); audit(access,"OTP_VERIFIED",null);
        return Map.of("grant",grant,"status",state(access.getAssessment()),"access",summary(access.getAssessment(),access.getInvitation(),false,"VERIFIED",true));
    }

    @Transactional(readOnly=true)
    public Map<String,Object> status(Long assessmentId, String grant) { AssessmentAccess access=grant(assessmentId,grant); return Map.of("status",state(access.getAssessment()),"serverNow",LocalDateTime.now(),"startAt",access.getAssessment().getStartAt(),"endAt",access.getAssessment().getEndAt(),"assessment",summary(access.getAssessment(),access.getInvitation(),false,"VERIFIED",true)); }
    @Transactional public Map<String,Object> start(Long assessmentId,String grant) { AssessmentAccess access=grant(assessmentId,grant); if(!"LIVE".equals(state(access.getAssessment()))) throw new IllegalArgumentException("The assessment is not live yet."); Map<String,Object> response=new LinkedHashMap<>(assessmentService.startAuthorized(access.getParticipant(),assessmentId)); response.put("assessment",summary(access.getAssessment(),access.getInvitation(),true,"ACTIVE",true)); return response; }
    @Transactional(readOnly=true) public AssessmentParticipantIdentity participantForSession(String grant, Long sessionId) { AssessmentAccess access=grantForAnyAssessment(grant); if(access.getParticipant()==null)throw new AccessDeniedException("This assessment session is not authorized by the access grant.");AssessmentSession session=sessions.findAuthorizedById(sessionId).filter(s->s.getAssessment().getId().equals(access.getAssessment().getId())&&s.getParticipant().getId().equals(access.getParticipant().getId())).orElseThrow(()->new AccessDeniedException("This assessment session is not authorized by the access grant."));return new AssessmentParticipantIdentity(session.getParticipant().getId(),session.getParticipant().getUsername()); }

    private AssessmentAccess grant(Long assessmentId,String value){AssessmentAccess a=accesses.findAllForGrantValidation().stream().filter(x->x.getAssessment().getId().equals(assessmentId)).filter(x->x.getGrantHash()!=null&&encoder.matches(value==null?"":value,x.getGrantHash())).findFirst().orElseThrow(()->new AccessDeniedException("A verified assessment access grant is required."));if(a.getInvitation()!=null&&a.getInvitation().getStatus()==AssessmentEnums.InvitationStatus.CANCELLED)throw new AccessDeniedException("This assessment invitation has been revoked.");if(a.getGrantExpiresAt()==null||LocalDateTime.now().isAfter(a.getGrantExpiresAt()))throw new AccessDeniedException("This assessment access grant has expired.");return a;}
    private void enforceIpIssueLimit(Long assessmentId,String clientIp){String key=assessmentId+":"+(clientIp==null?"unknown":clientIp);Deque<LocalDateTime> attempts=ipIssues.computeIfAbsent(key,k->new ArrayDeque<>());synchronized(attempts){LocalDateTime cutoff=LocalDateTime.now().minusHours(1);while(!attempts.isEmpty()&&attempts.peekFirst().isBefore(cutoff))attempts.removeFirst();if(attempts.size()>=10)throw new IllegalArgumentException("Too many code requests. Try again later.");attempts.addLast(LocalDateTime.now());}}
    private void audit(AssessmentAccess access,String event,String clientIp){if(audits==null)return;AssessmentAccessAudit audit=new AssessmentAccessAudit();audit.setAssessment(access.getAssessment());audit.setAccess(access);audit.setEvent(event);audit.setIpHash(hashIp(clientIp));audits.save(audit);} private static String hashIp(String ip){if(ip==null||ip.isBlank())return null;try{return Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(ip.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(Exception ignored){return null;}}
    private AssessmentAccess grantForAnyAssessment(String value){AssessmentAccess a=accesses.findAllForGrantValidation().stream().filter(x->x.getGrantHash()!=null&&encoder.matches(value==null?"":value,x.getGrantHash())).findFirst().orElseThrow(()->new AccessDeniedException("A verified assessment access grant is required."));if(a.getInvitation()!=null&&a.getInvitation().getStatus()==AssessmentEnums.InvitationStatus.CANCELLED)throw new AccessDeniedException("This assessment invitation has been revoked.");if(a.getGrantExpiresAt()==null||LocalDateTime.now().isAfter(a.getGrantExpiresAt()))throw new AccessDeniedException("This assessment access grant has expired.");return a;}
    private AssessmentAccess access(Long id,Long assessmentId){return accesses.findByIdAndAssessmentId(id,assessmentId).orElseThrow(()->new AccessDeniedException("Assessment access request not found."));}
    private Assessment assessment(Long id){return assessments.findPreflightById(id).orElseThrow(()->new IllegalArgumentException("Assessment unavailable."));}
    private AssessmentInvitation invitation(Assessment a,String token){if(token==null||token.isBlank())return null;LocalDateTime now=LocalDateTime.now();return invitations.findForAccessByAssessmentId(a.getId()).stream().filter(i->i.getStatus()==AssessmentEnums.InvitationStatus.INVITED).filter(i->i.getAccessTokenHash()!=null&&i.getAccessTokenExpiresAt()!=null&&now.isBefore(i.getAccessTokenExpiresAt())&&encoder.matches(token,i.getAccessTokenHash())).findFirst().orElse(null);}
    private void assertAvailable(Assessment a){if(a.getStatus()==AssessmentEnums.Status.DRAFT||a.getStatus()==AssessmentEnums.Status.CANCELLED)throw new IllegalArgumentException("Assessment unavailable.");if(!LocalDateTime.now().isBefore(a.getEndAt()))throw new IllegalArgumentException("Assessment has ended. This assessment is no longer accepting attempts.");}
    private String state(Assessment a){return LocalDateTime.now().isBefore(a.getStartAt())?"UPCOMING":LocalDateTime.now().isBefore(a.getEndAt())?"LIVE":"ENDED";}
    private User resolveParticipant(AssessmentAccess access){if(access.getInvitation()!=null)return access.getInvitation().getUser();return users.findByEmailIgnoreCase(access.getEmail()).orElseGet(()->{User u=new User();u.setEmail(access.getEmail());u.setUsername("assessment-"+UUID.randomUUID().toString().replace("-","").substring(0,18));u.setPassword(encoder.encode(randomToken()));u.setRole(Role.ASSESSMENT_GUEST);u.setEmailVerified(true);return users.save(u);});}
    private AssessmentPreflightResponse summary(Assessment a,AssessmentInvitation invite,boolean includeQuestions,String registrationStatus,boolean participantEligible){List<AssessmentProblem> selected=problems.findByAssessmentIdOrderByDisplayOrderAsc(a.getId());List<Map<String,Object>> mcqs=publicMcqs(a);LocalDateTime now=LocalDateTime.now();return new AssessmentPreflightResponse(a.getId(),a.getTitle(),a.getOrganization(),a.getCreator().getUsername(),a.getDescription(),a.getInstructions(),a.getStartAt(),a.getEndAt(),java.time.Duration.between(a.getStartAt(),a.getEndAt()).toMinutes(),a.isFullscreenRequired(),a.isMicrophoneRequired(),a.isStrictProctoring(),a.isStrictProctoring(),a.getVisibility()==AssessmentEnums.Visibility.PRIVATE,invite==null?"":mask(invite.getUser().getEmail()),selected.size(),mcqs.size(),selected.size()+mcqs.size(),registrationStatus,participantEligible,"LIVE".equals(state(a)),now,includeQuestions?selected.stream().map(this::assessmentProblem).toList():null,includeQuestions?mcqs:null);}
    private Map<String,Object> assessmentProblem(AssessmentProblem selection){Problem p=selection.getProblem();Map<String,Object> item=new LinkedHashMap<>();item.put("id",p.getId());item.put("title",p.getTitle());item.put("description",p.getDescription());item.put("difficulty",p.getDifficulty());item.put("constraints",p.getConstraints());item.put("inputFormat",p.getInputFormat());item.put("outputFormat",p.getOutputFormat());item.put("examples",p.getExamples());item.put("tags",p.getTags());item.put("starterCode",p.getStarterCode());item.put("executionMode",p.getExecutionMode()==null?"STDIN":p.getExecutionMode().name());if(p.getFunctionSignature()!=null){Map<String,Object> signature=new LinkedHashMap<>();signature.put("functionName",p.getFunctionSignature().getFunctionName());signature.put("returnType",p.getFunctionSignature().getReturnType());signature.put("parameters",p.getFunctionSignature().getParameters().stream().map(parameter->Map.<String,Object>of("name",parameter.getName(),"type",parameter.getType(),"order",parameter.getParameterOrder())).toList());item.put("functionSignature",signature);}else item.put("functionSignature",null);item.put("points",selection.getPoints());item.put("order",selection.getDisplayOrder());return item;}
    private List<Map<String,Object>> publicMcqs(Assessment assessment){try{List<Map<String,Object>> raw=new com.fasterxml.jackson.databind.ObjectMapper().readValue(assessment.getMcqQuestions()==null?"[]":assessment.getMcqQuestions(),new com.fasterxml.jackson.core.type.TypeReference<List<Map<String,Object>>>(){});return raw.stream().map(question->{Map<String,Object> safe=new LinkedHashMap<>(question);safe.remove("correctOption");return safe;}).toList();}catch(Exception ignored){return List.of();}}
    private static String normalEmail(String email){if(email==null||email.isBlank())throw new IllegalArgumentException("Enter a valid email address.");return email.trim().toLowerCase(Locale.ROOT);} private static String clean(String value){return value==null?null:value.trim().replaceAll("[<>]","");} private static String randomToken(){byte[] b=new byte[32];RANDOM.nextBytes(b);return Base64.getUrlEncoder().withoutPadding().encodeToString(b);} private static String mask(String email){int at=email.indexOf('@');return at<2?"•••"+email.substring(Math.max(0,at)):email.charAt(0)+"••••"+email.substring(at);}
}
