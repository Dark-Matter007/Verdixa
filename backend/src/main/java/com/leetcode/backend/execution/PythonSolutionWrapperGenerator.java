package com.leetcode.backend.execution;

import com.fasterxml.jackson.databind.JsonNode;
import com.leetcode.backend.model.FunctionSignature;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class PythonSolutionWrapperGenerator implements SolutionWrapperGenerator {
    private final FunctionValueCodec codec;
    public PythonSolutionWrapperGenerator(FunctionValueCodec codec) { this.codec = codec; }
    @Override public String language() { return "python"; }

    @Override public String generate(FunctionSignature signature, String userSource, List<JsonNode> arguments) {
        List<String> literals = new ArrayList<>();
        for (int i=0;i<arguments.size();i++) literals.add(codec.pythonLiteral(arguments.get(i), signature.getParameters().get(i).getType()));
        return userSource + "\n\nimport json\n_result = " + pythonName(signature.getFunctionName()) + "(" + String.join(", ", literals) + ")\nprint(json.dumps(_result, separators=(',', ':')), end='')\n";
    }

    private String pythonName(String name) {
        return name.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(java.util.Locale.ROOT);
    }
}
