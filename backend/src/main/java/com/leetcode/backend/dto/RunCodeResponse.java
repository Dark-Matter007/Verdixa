package com.leetcode.backend.dto;
import java.util.List;
public record RunCodeResponse(String executionMode, List<RunCaseResult> testCases) { }
