package com.leetcode.backend.controller;
import com.leetcode.backend.dto.*;
import com.leetcode.backend.service.HintService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/problems/{problemId}/hints") public class HintController {
 private final HintService service; public HintController(HintService s){service=s;}
 @GetMapping public HintListResponse get(@PathVariable Long problemId,Authentication a){return service.userHints(problemId,a.getName());}
 @PostMapping("/{hintId}/reveal") public HintResponse reveal(@PathVariable Long problemId,@PathVariable Long hintId,Authentication a){return service.reveal(problemId,hintId,a.getName());}
 @GetMapping("/admin") public List<HintResponse> admin(@PathVariable Long problemId){return service.admin(problemId);}
 @PostMapping public ResponseEntity<HintResponse> create(@PathVariable Long problemId,@Valid @RequestBody HintRequest r){return ResponseEntity.status(HttpStatus.CREATED).body(service.create(problemId,r));}
 @PutMapping("/{hintId}") public HintResponse update(@PathVariable Long problemId,@PathVariable Long hintId,@Valid @RequestBody HintRequest r){return service.update(problemId,hintId,r);}
 @PutMapping("/reorder") public List<HintResponse> reorder(@PathVariable Long problemId,@RequestBody List<Long> ids){return service.reorder(problemId,ids);}
 @DeleteMapping("/{hintId}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long problemId,@PathVariable Long hintId){service.delete(problemId,hintId);}
}
