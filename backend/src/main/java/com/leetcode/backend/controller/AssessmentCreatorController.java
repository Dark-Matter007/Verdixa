package com.leetcode.backend.controller;
import com.leetcode.backend.service.AssessmentCreatorService; import org.springframework.http.*; import org.springframework.security.core.Authentication; import org.springframework.web.bind.annotation.*; import org.springframework.web.multipart.MultipartFile; import java.util.*;
@RestController @RequestMapping("/api/assessment-creator") public class AssessmentCreatorController {
 private final AssessmentCreatorService service; AssessmentCreatorController(AssessmentCreatorService s){service=s;}
 @PostMapping(value="/applications",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> apply(Authentication a,@RequestParam String fullName,@RequestParam String organization,@RequestParam String roleTitle,@RequestParam String purpose,@RequestParam(required=false) String website,@RequestParam(required=false) String organizationEmail,@RequestParam String intendedUse,@RequestParam String country,@RequestParam String documentType,@RequestPart MultipartFile document){return service.apply(a.getName(),fullName,organization,roleTitle,purpose,website,organizationEmail,intendedUse,country,documentType,document);}
 @PostMapping("/applications/{id}/verify-otp") public Map<String,Object> verify(Authentication a,@PathVariable Long id,@RequestBody Map<String,String> b){return service.verify(a.getName(),id,b.get("code"));}
 @PostMapping("/applications/{id}/resend-otp") @ResponseStatus(HttpStatus.NO_CONTENT) public void resend(Authentication a,@PathVariable Long id){service.resend(a.getName(),id);}
 @GetMapping("/me/status") public Map<String,Object> status(Authentication a){return service.status(a.getName());}
}
