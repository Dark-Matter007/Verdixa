package com.leetcode.backend.dto;

/** Payload used by the non-persisted Run Code endpoint. */
public class ExecutionTestRequest {

    private String language;
    private String sourceCode;
    private String input;

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }
}
