package com.leetcode.backend.execution;

import com.fasterxml.jackson.databind.JsonNode;
import com.leetcode.backend.model.FunctionSignature;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class FunctionExecutionService {
    private final FunctionValueCodec codec;
    private final CodeExecutionService executor;
    private final Map<String, SolutionWrapperGenerator> wrappers;

    public FunctionExecutionService(FunctionValueCodec codec, CodeExecutionService executor, List<SolutionWrapperGenerator> generators) {
        this.codec = codec; this.executor = executor; this.wrappers = new HashMap<>();
        generators.forEach(generator -> wrappers.put(generator.language(), generator));
    }

    public FunctionExecutionResult execute(String language, FunctionSignature signature, String userSource,
                                           String functionArguments, String expectedReturn) {
        codec.validateSignature(signature);
        if (userSource == null || userSource.isBlank()) throw new IllegalArgumentException("Solution source is required.");
        List<JsonNode> arguments = codec.parseArguments(functionArguments, signature.getParameters());
        if (expectedReturn != null) codec.parse(expectedReturn, signature.getReturnType());
        String normalizedLanguage = normalizeLanguage(language);
        SolutionWrapperGenerator wrapper = wrappers.get(normalizedLanguage);
        if (wrapper == null) throw new IllegalArgumentException("Supported languages are Java, C++, and Python.");
        String source = wrapper.generate(signature, userSource, arguments);
        ExecutionResult result = switch (normalizedLanguage) {
            case "java" -> executor.executeJava(source, "");
            case "cpp" -> executor.executeCpp(source, "");
            default -> executor.executePython(source, "");
        };
        if (!"EXECUTED".equals(result.getStatus())) return new FunctionExecutionResult(result.getStatus(), result.getOutput(), result.getErrorMessage(), result.getExecutionTimeMs(), false, "");
        try {
            String normalized = codec.normalize(result.getOutput(), signature.getReturnType());
            if (expectedReturn == null) return new FunctionExecutionResult("EXECUTED", result.getOutput(), "", result.getExecutionTimeMs(), false, normalized);
            boolean passed = codec.equivalent(normalized, expectedReturn, signature.getReturnType());
            return new FunctionExecutionResult(passed ? "EXECUTED" : "WRONG_ANSWER", result.getOutput(), passed ? "" : "Function result did not match the expected return.", result.getExecutionTimeMs(), passed, normalized);
        } catch (IllegalArgumentException ex) {
            return new FunctionExecutionResult("RUNTIME_ERROR", result.getOutput(), "Function returned an invalid " + signature.getReturnType() + " value.", result.getExecutionTimeMs(), false, "");
        }
    }

    private String normalizeLanguage(String language) {
        if (language == null) return "";
        return switch (language.trim().toLowerCase(Locale.ROOT)) {
            case "java" -> "java"; case "c++", "cpp", "c plus plus" -> "cpp"; case "python", "python3" -> "python"; default -> "";
        };
    }
}
