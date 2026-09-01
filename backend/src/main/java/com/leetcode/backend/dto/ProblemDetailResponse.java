package com.leetcode.backend.dto;

import com.leetcode.backend.model.FunctionParameter;
import com.leetcode.backend.model.FunctionSignature;
import com.leetcode.backend.model.Problem;
import java.util.List;

/** Safe, fully materialized problem payload for the user solver. */
public record ProblemDetailResponse(Long id, String title, String description, String difficulty,
        String constraints, String inputFormat, String outputFormat, String examples, String tags,
        String starterCode, String executionMode, boolean active, FunctionSignatureResponse functionSignature) {
    public static ProblemDetailResponse from(Problem problem) {
        return new ProblemDetailResponse(problem.getId(), problem.getTitle(), problem.getDescription(),
                problem.getDifficulty(), problem.getConstraints(), problem.getInputFormat(), problem.getOutputFormat(),
                problem.getExamples(), problem.getTags(), problem.getStarterCode(), problem.getExecutionMode().name(),
                problem.isActive(), FunctionSignatureResponse.from(problem.getFunctionSignature()));
    }

    public record FunctionSignatureResponse(String functionName, String returnType, List<ParameterResponse> parameters) {
        static FunctionSignatureResponse from(FunctionSignature signature) {
            if (signature == null) return null;
            return new FunctionSignatureResponse(signature.getFunctionName(), signature.getReturnType(),
                    signature.getParameters().stream().map(ParameterResponse::from).toList());
        }
    }

    public record ParameterResponse(String name, String type, int order) {
        static ParameterResponse from(FunctionParameter parameter) {
            return new ParameterResponse(parameter.getName(), parameter.getType(), parameter.getParameterOrder());
        }
    }
}
