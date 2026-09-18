package com.leetcode.backend.controller;

import com.leetcode.backend.dto.*;
import com.leetcode.backend.service.AssessmentAccessService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/** Anonymous routes are intentionally narrow: they only mint and use an assessment grant. */
@RestController
@RequestMapping("/api/assessment-access")
public class AssessmentAccessController {
    private final AssessmentAccessService access;
    public AssessmentAccessController(AssessmentAccessService access){this.access=access;}
    @GetMapping("/{assessmentId}") public AssessmentPreflightResponse overview(@PathVariable Long assessmentId,@RequestParam(required=false) String invite){return access.overview(assessmentId,invite);}
    @PostMapping("/{assessmentId}/request") public Map<String,Object> request(@PathVariable Long assessmentId,@Valid @RequestBody AssessmentAccessRequest request,HttpServletRequest http){String forwarded=http.getHeader("X-Forwarded-For");String clientIp=forwarded==null||forwarded.isBlank()?http.getRemoteAddr():forwarded.split(",")[0].trim();return access.request(assessmentId,request,clientIp);}
    @PostMapping("/{assessmentId}/verify-otp") public Map<String,Object> verify(@PathVariable Long assessmentId,@Valid @RequestBody AssessmentOtpRequest request){return access.verify(assessmentId,request.accessId(),request.otp());}
    @PostMapping("/{assessmentId}/resend-otp") public Map<String,Object> resend(@PathVariable Long assessmentId,@Valid @RequestBody AssessmentResendOtpRequest request){return access.resend(assessmentId,request.accessId());}
    @GetMapping("/{assessmentId}/status") public Map<String,Object> status(@PathVariable Long assessmentId,@RequestHeader(value="X-Assessment-Access",required=false) String grant){return access.status(assessmentId,grant);}
    @PostMapping("/{assessmentId}/start") public Map<String,Object> start(@PathVariable Long assessmentId,@RequestHeader(value="X-Assessment-Access",required=false) String grant){return access.start(assessmentId,grant);}
}
