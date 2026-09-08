package com.leetcode.backend.controller;
import com.leetcode.backend.repository.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/admin")
public class AdminGuidanceController {
    private final EditorialRepository editorials; private final SubmissionRepository submissions;private final UserCertificateRepository certificates;
    public AdminGuidanceController(EditorialRepository editorials,SubmissionRepository submissions,UserCertificateRepository certificates){this.editorials=editorials;this.submissions=submissions;this.certificates=certificates;}
    @GetMapping("/editorial-statuses") public Map<Long,String> statuses(@RequestParam List<Long> ids){
        check(ids);Map<Long,String> result=new LinkedHashMap<>();ids.forEach(id->result.put(id,"Missing"));
        editorials.statuses(ids).forEach(e->result.put(e.getProblemId(),e.isPublished()?"Published":"Draft"));return result;
    }
    public record UserMilestones(long solved,int highestMilestone,int certificates) {}
    @GetMapping("/user-milestones") public Map<Long,UserMilestones> milestones(@RequestParam List<Long> ids){
        check(ids);Map<Long,Long> counts=new HashMap<>();submissions.solvedByUsers(ids).forEach(v->counts.put(v.getUserId(),v.getSolved()));
        var awards=certificates.awardsByUsers(ids);Map<Long,UserMilestones> result=new LinkedHashMap<>();
        for(Long id:ids){var earned=awards.stream().filter(a->a.getUserId().equals(id)).toList();result.put(id,new UserMilestones(counts.getOrDefault(id,0L),earned.stream().mapToInt(a->a.getMilestone().getTarget()).max().orElse(0),earned.size()));}return result;
    }
    private void check(List<Long> ids){if(ids.isEmpty()||ids.size()>100)throw new IllegalArgumentException("Request between 1 and 100 IDs.");}
}
