package com.leetcode.backend.execution;

public record FunctionExecutionResult(String status, String output, String errorMessage,
                                      long executionTimeMs, boolean passed, String normalizedOutput) {
}
