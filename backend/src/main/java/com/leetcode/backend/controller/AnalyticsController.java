package com.leetcode.backend.controller;

import com.leetcode.backend.model.*;
import com.leetcode.backend.repository.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.*;
import java.util.stream.*;

/** Read-only, submission-derived analytics. Run Code has no row in submissions and therefore cannot affect these values. */
@RestController @RequestMapping("/api/analytics")
public class AnalyticsController {
 final UserRepository users; final SubmissionRepository submissions; final ProblemRepository problems;
 AnalyticsController(UserRepository u,SubmissionRepository s,ProblemRepository p){users=u;submissions=s;problems=p;}
 @GetMapping("/me") public Map<String,Object> mine(Authentication a){return user(users.findByUsername(a.getName()).orElseThrow());}
 @GetMapping("/users/{id}") public Map<String,Object> user(@PathVariable Long id,Authentication a){if(a.getAuthorities().stream().noneMatch(x->x.getAuthority().equals("ROLE_ADMIN")))throw new org.springframework.security.access.AccessDeniedException("Admin required.");return user(users.findById(id).orElseThrow());}
 private Map<String,Object> user(User u){List<Submission> all=submissions.findByUserIdOrderBySubmittedAtDesc(u.getId());Set<Long> solved=all.stream().filter(s->"ACCEPTED".equals(s.getStatus())).map(s->s.getProblem().getId()).collect(Collectors.toSet());Map<String,Long> langs=all.stream().collect(Collectors.groupingBy(Submission::getLanguage,Collectors.counting()));Map<String,Object> r=new LinkedHashMap<>();r.put("totalSolved",solved.size());r.put("easySolved",solved.stream().map(problems::findById).flatMap(Optional::stream).filter(p->"EASY".equalsIgnoreCase(p.getDifficulty())).count());r.put("mediumSolved",solved.stream().map(problems::findById).flatMap(Optional::stream).filter(p->"MEDIUM".equalsIgnoreCase(p.getDifficulty())).count());r.put("hardSolved",solved.stream().map(problems::findById).flatMap(Optional::stream).filter(p->"HARD".equalsIgnoreCase(p.getDifficulty())).count());long accepted=all.stream().filter(s->"ACCEPTED".equals(s.getStatus())).count();r.put("totalSubmissions",all.size());r.put("acceptedSubmissions",accepted);r.put("acceptanceRate",all.isEmpty()?0:Math.round(accepted*100.0/all.size()));r.put("languageUsage",langs);r.put("heatmap",heatmap(all));return r;}
 private Map<String,Object> heatmap(List<Submission> all){LocalDate end=LocalDate.now(),start=end.minusDays(364);Map<LocalDate,List<Submission>> days=all.stream().filter(s->!s.getSubmittedAt().toLocalDate().isBefore(start)).collect(Collectors.groupingBy(s->s.getSubmittedAt().toLocalDate()));List<Map<String,Object>> cells=new ArrayList<>();for(LocalDate d=start;!d.isAfter(end);d=d.plusDays(1)){List<Submission> x=days.getOrDefault(d,List.of());cells.add(Map.of("date",d.toString(),"submissions",x.size(),"accepted",x.stream().filter(s->"ACCEPTED".equals(s.getStatus())).count()));}Set<LocalDate> active=days.keySet();return Map.of("days",cells,"activeDays",active.size(),"submissions",all.stream().filter(s->!s.getSubmittedAt().toLocalDate().isBefore(start)).count(),"accepted",all.stream().filter(s->!s.getSubmittedAt().toLocalDate().isBefore(start)&&"ACCEPTED".equals(s.getStatus())).count(),"currentStreak",streak(active,end),"longestStreak",longest(active));}
 private int streak(Set<LocalDate>x,LocalDate d){if(!x.contains(d))d=d.minusDays(1);int n=0;while(x.contains(d)){n++;d=d.minusDays(1);}return n;} private int longest(Set<LocalDate>x){int best=0;for(LocalDate d:x){if(x.contains(d.minusDays(1)))continue;int n=0;while(x.contains(d)){n++;d=d.plusDays(1);}best=Math.max(best,n);}return best;}
}
