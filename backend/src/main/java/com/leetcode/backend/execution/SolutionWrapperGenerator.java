package com.leetcode.backend.execution;

import com.leetcode.backend.model.FunctionSignature;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;

public interface SolutionWrapperGenerator {
    String language();
    String generate(FunctionSignature signature, String userSource, List<JsonNode> arguments);
}
