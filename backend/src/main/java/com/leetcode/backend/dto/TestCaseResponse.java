package com.leetcode.backend.dto;

import com.leetcode.backend.model.TestCase;
public record TestCaseResponse(Long id, String inputData, String functionArguments, String expectedOutput, boolean hidden) {
    public static TestCaseResponse publicCase(TestCase testCase) {
        if (testCase.isHidden()) throw new IllegalArgumentException("Hidden test cases cannot be exposed.");
        return new TestCaseResponse(testCase.getId(),testCase.getInputData(),testCase.getFunctionArguments(),testCase.getExpectedOutput(),false);
    }

    /** Administrative DTO: deliberately has no entity relationship fields. */
    public static TestCaseResponse adminCase(TestCase testCase) {
        return new TestCaseResponse(testCase.getId(), testCase.getInputData(), testCase.getFunctionArguments(),
                testCase.getExpectedOutput(), testCase.isHidden());
    }
}
