package com.leetcode.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RunCaseResult(int caseNumber, String status, Object actual, Object expected,
                            Boolean passed, long runtimeMs, String message) { }
