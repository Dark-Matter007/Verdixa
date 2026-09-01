package com.leetcode.backend.controller;

import com.leetcode.backend.model.*;
import com.leetcode.backend.repository.*;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/** Ordered curriculum management. Progress is intentionally calculated from submissions, not stored here. */
@RestController @RequestMapping("/api/learning-paths")
public class LearningPathController {
 final LearningPathRepository paths; final LearningPathSectionRepository sections; final LearningPathItemRepository items; final ProblemRepository problems;
 LearningPathController(LearningPathRepository p, LearningPathSectionRepository s, LearningPathItemRepository i, ProblemRepository r){paths=p;sections=s;items=i;problems=r;}
 void admin(Authentication a){if(a.getAuthorities().stream().noneMatch(x->x.getAuthority().equals("ROLE_ADMIN")))throw new org.springframework.security.access.AccessDeniedException("Admin required.");}
 boolean isAdmin(Authentication a){return a!=null&&a.getAuthorities().stream().anyMatch(x->x.getAuthority().equals("ROLE_ADMIN"));}
 @GetMapping @Transactional(readOnly=true) public List<LearningPath> all(Authentication a){return isAdmin(a)?paths.findAll():paths.findByPublishedTrue();}
 @GetMapping("/{id}") @Transactional(readOnly=true) public LearningPath one(@PathVariable Long id,Authentication a){LearningPath p=paths.findById(id).orElseThrow();if(!isAdmin(a)&&!p.isPublished())throw new org.springframework.security.access.AccessDeniedException("Published paths only.");return p;}
 @PostMapping public LearningPath create(@RequestBody LearningPath b,Authentication a){admin(a);b.setPublished(false);return paths.save(b);}
 @PutMapping("/{id}") public LearningPath update(@PathVariable Long id,@RequestBody LearningPath b,Authentication a){admin(a);LearningPath p=paths.findById(id).orElseThrow();p.setTitle(b.getTitle());p.setDescription(b.getDescription());p.setPublished(b.isPublished());return paths.save(p);}
 @DeleteMapping("/{id}") public void delete(@PathVariable Long id,Authentication a){admin(a);paths.delete(paths.findById(id).orElseThrow());}
 @PostMapping("/{id}/sections") public LearningPathSection addSection(@PathVariable Long id,@RequestBody LearningPathSection b,Authentication a){admin(a);LearningPath p=paths.findById(id).orElseThrow();b.setPath(p);b.setPosition(p.getSections().size());return sections.save(b);}
 @PutMapping("/sections/{sectionId}") public LearningPathSection updateSection(@PathVariable Long sectionId,@RequestBody LearningPathSection b,Authentication a){admin(a);LearningPathSection s=sections.findById(sectionId).orElseThrow();s.setTitle(b.getTitle());return sections.save(s);}
 @DeleteMapping("/sections/{sectionId}") public void deleteSection(@PathVariable Long sectionId,Authentication a){admin(a);sections.delete(sections.findById(sectionId).orElseThrow());}
 @PutMapping("/{id}/sections/order") @Transactional public LearningPath reorderSections(@PathVariable Long id,@RequestBody List<Long> ids,Authentication a){admin(a);LearningPath p=paths.findById(id).orElseThrow();if(ids.size()!=p.getSections().size()||!new HashSet<>(ids).equals(p.getSections().stream().map(LearningPathSection::getId).collect(java.util.stream.Collectors.toSet())))throw new IllegalArgumentException("Order must contain every section exactly once.");for(int n=0;n<ids.size();n++)sections.findById(ids.get(n)).orElseThrow().setPosition(n);return paths.save(p);}
 @PostMapping("/sections/{sectionId}/problems/{problemId}") public LearningPathItem addProblem(@PathVariable Long sectionId,@PathVariable Long problemId,Authentication a){admin(a);if(items.existsBySectionIdAndProblemId(sectionId,problemId))throw new IllegalArgumentException("Problem already exists in section.");LearningPathSection s=sections.findById(sectionId).orElseThrow();LearningPathItem i=new LearningPathItem();i.setSection(s);i.setProblem(problems.findById(problemId).orElseThrow());i.setPosition(s.getItems().size());return items.save(i);}
 @DeleteMapping("/sections/{sectionId}/problems/{problemId}") @Transactional public void removeProblem(@PathVariable Long sectionId,@PathVariable Long problemId,Authentication a){admin(a);LearningPathItem item=items.findAll().stream().filter(i->i.getSection().getId().equals(sectionId)&&i.getProblem().getId().equals(problemId)).findFirst().orElseThrow();items.delete(item);}
 @PutMapping("/sections/{sectionId}/problems/order") @Transactional public LearningPathSection reorderProblems(@PathVariable Long sectionId,@RequestBody List<Long> ids,Authentication a){admin(a);LearningPathSection s=sections.findById(sectionId).orElseThrow();if(ids.size()!=s.getItems().size()||!new HashSet<>(ids).equals(s.getItems().stream().map(LearningPathItem::getProblem).map(Problem::getId).collect(java.util.stream.Collectors.toSet())))throw new IllegalArgumentException("Order must contain every assigned problem exactly once.");for(int n=0;n<ids.size();n++){Long pid=ids.get(n);s.getItems().stream().filter(i->i.getProblem().getId().equals(pid)).findFirst().orElseThrow().setPosition(n);}return sections.save(s);}
}
