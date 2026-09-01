package com.leetcode.backend.execution;

public class ExecutionResult {

    private final String status;
    private final String output;
    private final String errorMessage;
    private final long executionTimeMs;

    public ExecutionResult(
            String status,
            String output,
            String errorMessage,
            long executionTimeMs) {

        this.status = status;
        this.output = output;
        this.errorMessage = errorMessage;
        this.executionTimeMs = executionTimeMs;
    }

    public String getStatus() {
        return status;
    }

    public String getOutput() {
        return output;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }
}