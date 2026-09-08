package com.leetcode.backend.controller;
import com.leetcode.backend.dto.*;
import com.leetcode.backend.repository.UserRepository;
import com.leetcode.backend.service.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController
@RequestMapping("/api")
public class CertificateController {
    private final CertificateService service;private final CertificatePdfService pdf;private final UserRepository users;
    public CertificateController(CertificateService service,CertificatePdfService pdf,UserRepository users){this.service=service;this.pdf=pdf;this.users=users;}
    @GetMapping("/users/me/certificate-progress") public CertificateProgressResponse mine(Authentication a){return progress(users.findByUsername(a.getName()).orElseThrow().getId());}
    @GetMapping("/admin/users/{id}/certificate-progress") public CertificateProgressResponse user(@PathVariable Long id){return progress(id);}
    // Reconcile missed awards after transient post-judge failures, without replaying celebrations.
    private CertificateProgressResponse progress(Long id){
        var p=service.evaluate(id);return new CertificateProgressResponse(p.solvedCount(),p.currentMilestone(),p.nextMilestone(),p.milestones(),java.util.List.of());
    }
    @GetMapping("/admin/certificates/summary") public Map<String,Long> summary(){return service.summary();}
    @GetMapping("/certificates/{publicId}") public CertificateResponse verify(@PathVariable String publicId){return service.verify(publicId);}
    @GetMapping(value="/certificates/{publicId}/pdf",produces=MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdf(@PathVariable String publicId) throws java.io.IOException {
        CertificateResponse certificate=service.verify(publicId);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=verdixa-"+certificate.publicCertificateId()+".pdf")
            .contentType(MediaType.APPLICATION_PDF).body(pdf.generate(certificate));
    }
}
