package com.leetcode.backend.controller;

import com.leetcode.backend.execution.CodeExecutionService;
import com.leetcode.backend.execution.ExecutionResult;
import com.leetcode.backend.dto.ExecutionTestRequest;
import com.leetcode.backend.dto.RunCodeRequest;
import com.leetcode.backend.dto.RunCodeResponse;
import com.leetcode.backend.service.RunCodeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/execution-test")
public class ExecutionTestController {
    private static final int MAX_SOURCE_CHARS = 200_000;
    private static final int MAX_INPUT_CHARS = 64_000;

    private final CodeExecutionService codeExecutionService;
    private final RunCodeService runCodeService;

    public ExecutionTestController(
            CodeExecutionService codeExecutionService, RunCodeService runCodeService) {

        this.codeExecutionService = codeExecutionService;
        this.runCodeService = runCodeService;
    }

    @PostMapping("/run")
    public ResponseEntity<RunCodeResponse> run(@RequestBody RunCodeRequest request) {
        return ResponseEntity.ok(runCodeService.run(request));
    }

    // ==========================
    // JAVA EXECUTION
    // ==========================

    @PostMapping("/java")
    public ResponseEntity<ExecutionResult> executeJava(@RequestBody ExecutionTestRequest request) {
        validate(request);
        return ResponseEntity.ok(codeExecutionService.executeJava(
                request.getSourceCode(), request.getInput() == null ? "" : request.getInput()));
    }

    // ==========================
    // C++ EXECUTION
    // ==========================

    @PostMapping("/cpp")
    public ResponseEntity<ExecutionResult> executeCpp(@RequestBody ExecutionTestRequest request) {
        validate(request);
        return ResponseEntity.ok(codeExecutionService.executeCpp(
                request.getSourceCode(), request.getInput() == null ? "" : request.getInput()));
    }

    // ==========================
    // PYTHON EXECUTION
    // ==========================

    @PostMapping("/python")
    public ResponseEntity<ExecutionResult> executePython(@RequestBody ExecutionTestRequest request) {
        validate(request);
        return ResponseEntity.ok(codeExecutionService.executePython(
                request.getSourceCode(), request.getInput() == null ? "" : request.getInput()));
    }
    private void validate(ExecutionTestRequest request) {
        if (request == null || request.getSourceCode() == null || request.getSourceCode().isBlank() || request.getSourceCode().length() > MAX_SOURCE_CHARS) throw new IllegalArgumentException("Source code must be between 1 and 200000 characters.");
        if (request.getInput() != null && request.getInput().length() > MAX_INPUT_CHARS) throw new IllegalArgumentException("Custom input is too large.");
    }
}
