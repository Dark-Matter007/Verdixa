package com.leetcode.backend.execution;

import com.fasterxml.jackson.databind.JsonNode;
import com.leetcode.backend.model.FunctionSignature;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class JavaSolutionWrapperGenerator implements SolutionWrapperGenerator {
    private final FunctionValueCodec codec;
    public JavaSolutionWrapperGenerator(FunctionValueCodec codec) { this.codec = codec; }
    @Override public String language() { return "java"; }

    @Override public String generate(FunctionSignature signature, String userSource, List<JsonNode> arguments) {
        List<String> literals = new ArrayList<>();
        for (int i = 0; i < arguments.size(); i++) literals.add(codec.javaLiteral(arguments.get(i), signature.getParameters().get(i).getType()));
        return """
                import java.util.*;
                public class Solution {
                %s
                public static void main(String[] args) {
                    Solution solution = new Solution();
                    %s result = solution.%s(%s);
                    System.out.print(toJson(result));
                }
                private static String escape(String s) { StringBuilder b=new StringBuilder("\\\""); for(char c:s.toCharArray()) { switch(c) { case '\\\\' -> b.append("\\\\\\\\"); case '\"' -> b.append("\\\\\\\""); case '\\n' -> b.append("\\\\n"); case '\\r' -> b.append("\\\\r"); case '\\t' -> b.append("\\\\t"); default -> { if(c < 32) b.append(String.format("\\\\u%%04x",(int)c)); else b.append(c); } } } return b.append('\\\"').toString(); }
                private static String toJson(int v) { return Integer.toString(v); }
                private static String toJson(long v) { return Long.toString(v); }
                private static String toJson(double v) { return Double.toString(v); }
                private static String toJson(boolean v) { return Boolean.toString(v); }
                private static String toJson(String v) { return escape(v); }
                private static String toJson(int[] v) { return Arrays.toString(v).replace(" ", ""); }
                private static String toJson(long[] v) { return Arrays.toString(v).replace(" ", ""); }
                private static String toJson(double[] v) { return Arrays.toString(v).replace(" ", ""); }
                private static String toJson(String[] v) { StringJoiner j=new StringJoiner(",","[","]"); for(String s:v) j.add(escape(s)); return j.toString(); }
                }
                """.formatted(userSource, signature.getReturnType(), signature.getFunctionName(), String.join(", ", literals));
    }
}
