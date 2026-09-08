package com.leetcode.backend.controller;
import com.leetcode.backend.dto.*;
import com.leetcode.backend.model.*;
import com.leetcode.backend.repository.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/problems/{problemId}/editorial") public class EditorialController {
 private final com.leetcode.backend.service.EditorialAccessService access;
 private final EditorialRepository editorials; private final ProblemRepository problems;
 public EditorialController(EditorialRepository e,ProblemRepository p,com.leetcode.backend.service.EditorialAccessService access){editorials=e;problems=p;this.access=access;}
 @GetMapping public Object get(@PathVariable Long problemId,Authentication a){return isAdmin(a) ? EditorialResponse.from(find(problemId)) : access.access(problemId,a.getName());}
 @PostMapping("/reveal") public EditorialAccessResponse reveal(@PathVariable Long problemId,Authentication a){return access.reveal(problemId,a.getName());}
 @PutMapping @org.springframework.transaction.annotation.Transactional public EditorialResponse put(@PathVariable Long problemId,@Valid @RequestBody EditorialRequest body){problems.lockById(problemId).orElseThrow();Editorial value=editorials.findByProblemId(problemId).orElseGet(Editorial::new);value.setProblem(problems.findById(problemId).orElseThrow(()->new RuntimeException("Problem not found.")));copy(value,body);return EditorialResponse.from(editorials.save(value));}
 @DeleteMapping @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long problemId){editorials.delete(find(problemId));}
 private Editorial find(Long pid){return editorials.findByProblemId(pid).orElseThrow(()->new RuntimeException("Editorial not found."));}
 private boolean isAdmin(Authentication a){return a.getAuthorities().stream().anyMatch(x->x.getAuthority().equals("ROLE_ADMIN"));}
 private void copy(Editorial e,EditorialRequest r){e.setTitle(r.title().trim());e.setIntuition(r.intuition().trim());e.setApproach(r.approach().trim());e.setAlgorithmExplanation(r.algorithmExplanation().trim());e.setEdgeCases(r.edgeCases());e.setTimeComplexity(r.timeComplexity().trim());e.setSpaceComplexity(r.spaceComplexity().trim());e.setJavaSolution(r.javaSolution());e.setCppSolution(r.cppSolution());e.setPythonSolution(r.pythonSolution());e.setPublished(r.published());e.setKeyObservations(r.keyObservations());e.setCorrectness(r.correctness());e.setReferenceGuidance(r.referenceGuidance());e.setContentSource("MANUAL");}
}
