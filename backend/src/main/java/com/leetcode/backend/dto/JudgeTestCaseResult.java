package com.leetcode.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record JudgeTestCaseResult(int testNumber, boolean hidden, Object arguments, String input,
                                  Object expected, Object actual, String status, long runtimeMs, String message) {
    public static JudgeTestCaseResult hidden(int number,String status,long runtime) {
        String message="EXECUTED".equals(status)?"Hidden Test " + number + " passed.":"Hidden Test " + number + " — " + status;
        return new JudgeTestCaseResult(number,true,null,null,null,null,status,runtime,message);
    }
}
