package com.leetcode.backend.dto;

import java.util.List;

public class RunCodeRequest {
    private Long problemId;
    private String language;
    private String sourceCode;
    private String input;
    private List<CustomRunCase> testCases;
    public Long getProblemId() { return problemId; } public void setProblemId(Long value) { problemId=value; }
    public String getLanguage() { return language; } public void setLanguage(String value) { language=value; }
    public String getSourceCode() { return sourceCode; } public void setSourceCode(String value) { sourceCode=value; }
    public String getInput() { return input; } public void setInput(String value) { input=value; }
    public List<CustomRunCase> getTestCases() { return testCases; } public void setTestCases(List<CustomRunCase> value) { testCases=value; }
    public static class CustomRunCase {
        /* Keep the HTTP boundary Jackson-version-neutral; FunctionValueCodec validates the JSON contract. */
        private Object arguments;
        private Object expected;
        public Object getArguments() { return arguments; } public void setArguments(Object value) { arguments=value; }
        public Object getExpected() { return expected; } public void setExpected(Object value) { expected=value; }
    }
}
