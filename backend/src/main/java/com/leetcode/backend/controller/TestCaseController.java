package com.leetcode.backend.controller;

import com.leetcode.backend.model.TestCase;
import com.leetcode.backend.service.TestCaseService;
import com.leetcode.backend.dto.TestCaseResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/problems/{problemId}/testcases")
public class TestCaseController {

    private final TestCaseService testCaseService;

    public TestCaseController(TestCaseService testCaseService) {
        this.testCaseService = testCaseService;
    }

    // USER + ADMIN
    // Returns only public test cases.
    @GetMapping
    public ResponseEntity<List<TestCaseResponse>> getPublicTestCases(
            @PathVariable Long problemId) {

        return ResponseEntity.ok(
                testCaseService.getPublicTestCases(problemId).stream().map(TestCaseResponse::publicCase).toList()
        );
    }

    // ADMIN ONLY
    // Returns public + hidden test cases.
    @GetMapping("/all")
    public ResponseEntity<List<TestCaseResponse>> getAllTestCases(
            @PathVariable Long problemId) {

        return ResponseEntity.ok(
                testCaseService.getAllTestCases(problemId).stream().map(TestCaseResponse::adminCase).toList()
        );
    }

    // ADMIN ONLY
    @PostMapping
    public ResponseEntity<TestCaseResponse> createTestCase(
            @PathVariable Long problemId,
            @RequestBody TestCase testCase) {

        TestCase createdTestCase =
                testCaseService.createTestCase(
                        problemId,
                        testCase
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(TestCaseResponse.adminCase(createdTestCase));
    }

    // ADMIN ONLY
    @PutMapping("/{testCaseId}")
    public ResponseEntity<TestCaseResponse> updateTestCase(
            @PathVariable Long problemId,
            @PathVariable Long testCaseId,
            @RequestBody TestCase testCase) {

        return ResponseEntity.ok(TestCaseResponse.adminCase(
                testCaseService.updateTestCase(
                        problemId,
                        testCaseId,
                        testCase
                ))
        );
    }

    // ADMIN ONLY
    @DeleteMapping("/{testCaseId}")
    public ResponseEntity<Void> deleteTestCase(
            @PathVariable Long problemId,
            @PathVariable Long testCaseId) {

        testCaseService.deleteTestCase(problemId, testCaseId);

        return ResponseEntity.noContent().build();
    }
}
