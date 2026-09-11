package com.leetcode.backend.service;

import com.leetcode.backend.dto.*;
import com.leetcode.backend.model.*;
import com.leetcode.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

/** Controlled admin read layer. It returns database-grounded values and never sends secrets or statistics to Gemini. */
@Service public class AdminAssistantService {
 private final UserRepository users; private final ProblemRepository problems; private final SubmissionRepository submissions; private final ContestRepository contests; private final UserCertificateRepository certificates; private final VerdixaAssistantScopeService scope;
 public AdminAssistantService(UserRepository users,ProblemRepository problems,SubmissionRepository submissions,ContestRepository contests,UserCertificateRepository certificates,VerdixaAssistantScopeService scope){this.users=users;this.problems=problems;this.submissions=submissions;this.contests=contests;this.certificates=certificates;this.scope=scope;}
 @Transactional(readOnly=true) public AssistantChatResponse chat(AssistantChatRequest request){
   String q=scope.normalize(request.message()); VerdixaAssistantScope intent=scope.classify(request,VerdixaAssistantAudience.ADMIN);
   if(intent==VerdixaAssistantScope.SENSITIVE_REQUEST)return answer(scope.sensitiveMessage(),null);
   if(intent==VerdixaAssistantScope.OUT_OF_SCOPE)return answer(scope.outOfScopeMessage(),null);
   if(intent==VerdixaAssistantScope.ADMIN_ACTION)return contestSuggestion(q);
   if(isUserIntent(intent))return answer("I can provide safe platform-level administration information, but I cannot access an individual user's private assistant context from the admin assistant.","/admin/users");
   String publicHelp=scope.publicHelpAnswer(q,request.pageType()); if(publicHelp!=null)return answer(publicHelp,publicRoute(q));
   if(intent==VerdixaAssistantScope.PUBLIC_BENEFITS)return answer(scope.benefitsMessage(),null);
   if(q.matches(".*\\b(delete|change role|reset password|publish|unpublish)\\b.*"))return answer("I can guide you to the existing administration page, but I cannot perform that action through chat.",q.contains("contest")?"/admin/contests":"/admin");
   long totalUsers=users.count(), activeProblems=problems.countByActiveTrue(), totalSubmissions=submissions.count(), accepted=submissions.countByStatus("ACCEPTED"), totalContests=contests.count();
   List<Problem> active=problems.findByActiveTrue(); long easy=active.stream().filter(p->"EASY".equalsIgnoreCase(p.getDifficulty())).count(), medium=active.stream().filter(p->"MEDIUM".equalsIgnoreCase(p.getDifficulty())).count(), hard=active.stream().filter(p->"HARD".equalsIgnoreCase(p.getDifficulty())).count();
   List<Contest> allContests=contests.findAll(); long live=allContests.stream().filter(c->c.derivedStatus()==ContestStatus.LIVE).count(), upcoming=allContests.stream().filter(c->c.derivedStatus()==ContestStatus.UPCOMING).count(), completed=allContests.stream().filter(c->c.derivedStatus()==ContestStatus.ENDED).count();
   if(intent==VerdixaAssistantScope.ADMIN_SUMMARY)return answer("Platform summary: "+totalUsers+" users, "+activeProblems+" active problems, "+totalSubmissions+" submissions, and "+accepted+" accepted submissions. Contests: "+live+" live, "+upcoming+" upcoming, and "+completed+" completed.","/admin");
   if(q.contains("users performing"))return answer("Verdixa has "+totalUsers+" users and "+totalSubmissions+" submissions, including "+accepted+" accepted submissions. Open analytics for detailed authorized performance views.","/admin");
   if(q.contains("user")||q.contains("people use"))return answer("Verdixa currently has "+totalUsers+" registered users.","/admin/users");
   if(q.contains("problem")||q.matches(".*\\b(easy|medium|hard)\\b.*"))return answer("There are "+activeProblems+" active problems: "+easy+" Easy, "+medium+" Medium, and "+hard+" Hard.","/admin/problems");
   if(q.contains("submission")||q.contains("platform summary"))return answer("Platform summary: "+totalUsers+" users, "+activeProblems+" active problems, "+totalSubmissions+" submissions, and "+accepted+" accepted submissions.","/admin");
   if(q.contains("contest")||q.contains("registrations")){List<ContestRepository.AdminContestRow> rows=contests.findAdminRows(); if(q.contains("most registrations")&&!rows.isEmpty()){ContestRepository.AdminContestRow top=rows.stream().max(Comparator.comparingLong(ContestRepository.AdminContestRow::getRegistrationCount)).orElseThrow();return answer(top.getTitle()+" has the most registrations with "+top.getRegistrationCount()+" registered users.","/admin/contests");}String latest=rows.stream().limit(3).map(c->c.getTitle()+" ("+c.getRegistrationCount()+" registered)").collect(Collectors.joining(", ")); return answer("Verdixa has "+totalContests+" contests: "+live+" live, "+upcoming+" upcoming, and "+completed+" completed."+(latest.isBlank()?"":" Recent contests: "+latest+"."),"/admin/contests");}
   if(q.contains("certificate"))return answer("Verdixa has issued "+certificates.count()+" certificates across configured milestones.","/admin");
   if(intent==VerdixaAssistantScope.ADMIN_DATA)return answer("Platform summary: "+totalUsers+" users, "+activeProblems+" active problems, "+totalSubmissions+" submissions, and "+accepted+" accepted submissions.","/admin");
   return answer("I can help with database-grounded Verdixa totals, problem distribution, submissions, contests, certificates, safe navigation, and contest planning. I cannot expose secrets or execute actions through chat.","/admin");
 }
 private AssistantChatResponse contestSuggestion(String q){
   List<Problem> active=problems.findByActiveTrue(); List<Problem> balanced=new ArrayList<>(); if(q.contains("beginner")){active.stream().filter(p->"EASY".equalsIgnoreCase(p.getDifficulty())||"MEDIUM".equalsIgnoreCase(p.getDifficulty())).limit(3).forEach(balanced::add);}else for(String difficulty:List.of("EASY","MEDIUM","HARD"))active.stream().filter(p->difficulty.equalsIgnoreCase(p.getDifficulty())).findFirst().ifPresent(balanced::add);
   if(balanced.isEmpty())return answer("There are no active problems available for a contest draft yet. Create and publish problems first.","/admin/problems");
   String names=balanced.stream().map(Problem::getTitle).collect(Collectors.joining(", "));
   return answer("A balanced contest starting point is one Easy, one Medium, and one Hard active problem: "+names+". Review the schedule, visibility, scoring, and selected problems on the Contest administration page before creating it.","/admin/contests");
 }
 private static AssistantChatResponse answer(String message,String route){return new AssistantChatResponse(message,route);}
 private static boolean isUserIntent(VerdixaAssistantScope intent){return intent==VerdixaAssistantScope.USER_DATA||intent==VerdixaAssistantScope.USER_SUMMARY||intent==VerdixaAssistantScope.USER_SUBMISSIONS||intent==VerdixaAssistantScope.USER_CONTESTS||intent==VerdixaAssistantScope.USER_CERTIFICATES;}
 private static String publicRoute(String q){if(q.contains("password"))return "/forgot-password";if(q.matches(".*\\b(sign up|signup|register)\\b.*"))return "/register";if(q.matches(".*\\b(login|log in|sign in)\\b.*"))return "/login";if(q.contains("contest"))return "/admin/contests";if(q.contains("problem"))return "/admin/problems";return null;}
}
