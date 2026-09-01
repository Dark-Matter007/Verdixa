package com.leetcode.backend.execution;

import com.leetcode.backend.model.FunctionSignature;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SolutionWrapperGeneratorTest {
    private final FunctionValueCodec codec = new FunctionValueCodec();

    @Test void javaWrapperKeepsSolutionOnlyMethodAndAddsDriver() {
        FunctionSignature signature = signature("reverseString", "String", "s", "String");
        String source = new JavaSolutionWrapperGenerator(codec).generate(signature,
                "public String reverseString(String s) { return new StringBuilder(s).reverse().toString(); }",
                codec.parseArguments("[\"hello\"]", signature.getParameters()));
        assertTrue(source.contains("public class Solution"));
        assertTrue(source.contains("solution.reverseString(\"hello\")"));
        assertTrue(source.contains("System.out.print(toJson(result))"));
    }

    @Test void wrappersGenerateArrayAndEscapedArguments() {
        FunctionSignature signature = signature("echo", "String[]", "items", "String[]");
        var arguments = codec.parseArguments("[[\"a\\n\",\"b\\\"\"]]", signature.getParameters());
        assertTrue(new JavaSolutionWrapperGenerator(codec).generate(signature, "public String[] echo(String[] items){return items;}", arguments).contains("new String[]{\"a\\n\", \"b\\\"\"}"));
        assertTrue(new CppSolutionWrapperGenerator(codec).generate(signature, "vector<string> echo(vector<string> v){return v;}", arguments).contains("{\"a\\n\", \"b\\\"\"}"));
        assertTrue(new PythonSolutionWrapperGenerator(codec).generate(signature, "def echo(items): return items", arguments).contains("echo(['a\\n', 'b\"'])"));
    }

    static FunctionSignature signature(String name, String returnType, String parameterName, String parameterType) {
        FunctionSignature s = new FunctionSignature(); s.setFunctionName(name); s.setReturnType(returnType);
        s.setParameters(java.util.List.of(FunctionValueCodecTest.parameter(parameterName, parameterType))); return s;
    }
}
