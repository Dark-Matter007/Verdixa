package com.leetcode.backend.service;
import com.leetcode.backend.dto.*;
import com.leetcode.backend.model.*;
import com.leetcode.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
@Service
public class CertificateService {
    private final UserRepository users;
    private final SubmissionRepository submissions;
    private final UserCertificateRepository certificates;
    public CertificateService(UserRepository users,SubmissionRepository submissions,UserCertificateRepository certificates) {
        this.users=users;this.submissions=submissions;this.certificates=certificates;
    }
    // The lock is acquired before the first count. READ_COMMITTED also avoids stale
    // repeatable-read snapshots when two judges finish on different app instances.
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public CertificateProgressResponse evaluate(Long userId) {
        User user=users.lockById(userId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"User not found."));
        long solved=submissions.countUniqueSolved(userId);
        List<UserCertificate> earned=new ArrayList<>(certificates.findByUserIdOrderByMilestoneAsc(userId));
        List<CertificateResponse> issued=new ArrayList<>();
        for(CertificateMilestone milestone:CertificateMilestone.values()) {
            if(solved>=milestone.getTarget() && earned.stream().noneMatch(c->c.getMilestone()==milestone)) {
                UserCertificate value=certificates.saveAndFlush(new UserCertificate(user,milestone,solved));
                earned.add(value);issued.add(CertificateResponse.from(value));
            }
        }
        return progress(solved,earned,issued);
    }
    @Transactional(readOnly=true)
    public CertificateProgressResponse progress(Long userId) {
        if(!users.existsById(userId))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"User not found.");
        return progress(submissions.countUniqueSolved(userId),certificates.findByUserIdOrderByMilestoneAsc(userId),List.of());
    }
    @Transactional(readOnly=true)
    public CertificateResponse verify(String publicId) {
        return CertificateResponse.from(certificates.findByPublicCertificateId(publicId)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Certificate not found.")));
    }
    @Transactional(readOnly=true)
    public Map<String,Long> summary() {
        Map<String,Long> result=new LinkedHashMap<>();long total=0;
        for(CertificateMilestone tier:CertificateMilestone.values()) {long count=certificates.countByMilestone(tier);result.put(Integer.toString(tier.getTarget()),count);total+=count;}
        result.put("total",total);return result;
    }
    private CertificateProgressResponse progress(long solved,List<UserCertificate> earned,List<CertificateResponse> issued) {
        List<CertificateProgressResponse.MilestoneProgress> tiers=new ArrayList<>();int highest=0;Integer next=null;
        for(CertificateMilestone tier:CertificateMilestone.values()) {
            int target=tier.getTarget();UserCertificate c=earned.stream().filter(v->v.getMilestone()==tier).findFirst().orElse(null);
            if(c!=null)highest=target;else if(next==null)next=target;
            tiers.add(new CertificateProgressResponse.MilestoneProgress(target,c!=null,Math.min(solved,target),target,Math.max(0,target-solved),
                c==null?null:c.getPublicCertificateId(),c==null?null:c.getIssuedAt(),c==null?null:"VERIFIED"));
        }
        return new CertificateProgressResponse(solved,highest,next,tiers,issued);
    }
}
