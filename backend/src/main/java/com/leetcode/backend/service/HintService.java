package com.leetcode.backend.service;
import com.leetcode.backend.dto.*;
import com.leetcode.backend.model.*;
import com.leetcode.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class HintService {
 private static final int ATTEMPTS_PER_HINT = 3;
 private final ProblemHintRepository hints; private final HintRevealRepository reveals; private final ProblemRepository problems; private final UserRepository users; private final SubmissionRepository submissions;
 public HintService(ProblemHintRepository h, HintRevealRepository r, ProblemRepository p, UserRepository u, SubmissionRepository s){hints=h;reveals=r;problems=p;users=u;submissions=s;}
 @Transactional public HintListResponse userHints(Long problemId, String username){
  User user=user(username); problem(problemId); List<ProblemHint> active=hints.findByProblemIdAndActiveTrueOrderByDisplayOrderAsc(problemId);
  long attempts=submissions.countByUserIdAndProblemId(user.getId(),problemId);
  Set<Long> revealed=reveals.findByUserIdAndProblemId(user.getId(),problemId).stream().map(x->x.getHint().getId()).collect(java.util.stream.Collectors.toSet());
  List<HintResponse> response=new ArrayList<>();
  for(int i=0;i<active.size();i++)response.add(HintResponse.user(active.get(i),i+1,attempts,revealed.contains(active.get(i).getId())));
  int nextThreshold=response.stream().filter(h->!h.revealed()).mapToInt(HintResponse::attemptsRequired).min().orElse(active.size()*ATTEMPTS_PER_HINT);
  boolean available=response.stream().anyMatch(h->h.available()&&!h.revealed());
  return new HintListResponse(active.size(), revealed.stream().filter(id->active.stream().anyMatch(h->h.getId().equals(id))).count(), attempts, nextThreshold, available, response);
 }
 @Transactional public HintResponse reveal(Long problemId, Long hintId, String username){
  User user=user(username); ProblemHint hint=hints.findById(hintId).orElseThrow(()->new RuntimeException("Hint not found."));
  if(!hint.isActive() || !hint.getProblem().getId().equals(problemId)) throw new IllegalArgumentException("Hint is unavailable.");
  List<ProblemHint> active=hints.findByProblemIdAndActiveTrueOrderByDisplayOrderAsc(problemId);
  int index=active.stream().map(ProblemHint::getId).toList().indexOf(hintId); if(index<0) throw new IllegalArgumentException("Hint is unavailable.");
  long attempts=submissions.countByUserIdAndProblemId(user.getId(),problemId); int attemptsRequired=(index+1)*ATTEMPTS_PER_HINT;
  if(attempts<attemptsRequired)throw new IllegalArgumentException("This hint unlocks after "+attemptsRequired+" submission attempts for this problem.");
  if(reveals.findByUserIdAndHintId(user.getId(),hintId).isPresent()) return HintResponse.user(hint,index+1,attempts,true);
  HintReveal reveal=new HintReveal(); reveal.setUser(user); reveal.setProblem(problem(problemId)); reveal.setHint(hint); reveals.save(reveal); return HintResponse.user(hint,index+1,attempts,true);
 }
 @Transactional public HintResponse create(Long problemId, HintRequest r){ List<ProblemHint> all=hints.findByProblemIdOrderByDisplayOrderAsc(problemId); ProblemHint h=new ProblemHint(); h.setProblem(problem(problemId)); h.setTitle(r.title().trim());h.setContent(r.content().trim());h.setPenaltyPoints(r.penaltyPoints()==null?0:r.penaltyPoints());h.setActive(r.active()==null||r.active());h.setDisplayOrder(all.size()+1);return HintResponse.admin(hints.save(h)); }
 @Transactional public HintResponse update(Long problemId,Long hintId,HintRequest r){ProblemHint h=owned(problemId,hintId);h.setTitle(r.title().trim());h.setContent(r.content().trim());h.setPenaltyPoints(r.penaltyPoints()==null?0:r.penaltyPoints());if(r.active()!=null)h.setActive(r.active());return HintResponse.admin(hints.save(h));}
 @Transactional public void delete(Long problemId,Long hintId){hints.delete(owned(problemId,hintId)); hints.flush(); reindex(problemId);}
 @Transactional public List<HintResponse> reorder(Long problemId,List<Long> ids){List<ProblemHint> all=hints.findByProblemIdOrderByDisplayOrderAsc(problemId);if(all.size()!=ids.size()||!new HashSet<>(ids).equals(all.stream().map(ProblemHint::getId).collect(java.util.stream.Collectors.toSet())))throw new IllegalArgumentException("Reorder must include every hint exactly once."); for(ProblemHint h:all){h.setDisplayOrder(-h.getDisplayOrder());hints.save(h);} hints.flush(); for(int i=0;i<ids.size();i++){ProblemHint h=owned(problemId,ids.get(i));h.setDisplayOrder(i+1);hints.save(h);} return hints.findByProblemIdOrderByDisplayOrderAsc(problemId).stream().map(HintResponse::admin).toList();}
 @Transactional(readOnly=true) public List<HintResponse> admin(Long pid){return hints.findByProblemIdOrderByDisplayOrderAsc(pid).stream().map(HintResponse::admin).toList();}
 private void reindex(Long pid){List<ProblemHint> all=hints.findByProblemIdOrderByDisplayOrderAsc(pid);for(int i=0;i<all.size();i++){all.get(i).setDisplayOrder(i+1);hints.save(all.get(i));}}
 private ProblemHint owned(Long pid,Long hid){ProblemHint h=hints.findById(hid).orElseThrow(()->new RuntimeException("Hint not found."));if(!h.getProblem().getId().equals(pid))throw new IllegalArgumentException("Hint does not belong to this problem.");return h;} private Problem problem(Long id){return problems.findById(id).orElseThrow(()->new RuntimeException("Problem not found."));} private User user(String n){return users.findByUsername(n).orElseThrow(()->new RuntimeException("Authenticated user not found."));}
}
