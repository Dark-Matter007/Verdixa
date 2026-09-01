package com.leetcode.backend.execution;

import com.leetcode.backend.model.FunctionSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class FunctionExecutionServiceTest {
    private FunctionExecutionService service;
    private CodeExecutionService codeExecutionService;

    @BeforeEach void setUp() {
        FunctionValueCodec codec = new FunctionValueCodec();
        codeExecutionService = new CodeExecutionService();
        ReflectionTestUtils.setField(codeExecutionService, "javacCommand", "javac");
        ReflectionTestUtils.setField(codeExecutionService, "javaCommand", "java");
        ReflectionTestUtils.setField(codeExecutionService, "gxxCommand", "C:\\msys64\\ucrt64\\bin\\g++.exe");
        ReflectionTestUtils.setField(codeExecutionService, "pythonCommand", "python");
        ReflectionTestUtils.setField(codeExecutionService, "runtimePath", "C:\\msys64\\ucrt64\\bin;C:\\msys64\\usr\\bin");
        service = new FunctionExecutionService(codec, codeExecutionService, List.of(
                new JavaSolutionWrapperGenerator(codec), new CppSolutionWrapperGenerator(codec), new PythonSolutionWrapperGenerator(codec)));
    }

    @Test void executesReverseStringInAllLanguages() {
        FunctionSignature javaSig = SolutionWrapperGeneratorTest.signature("reverseString", "String", "s", "String");
        assertExecuted(service.execute("Java", javaSig, "public String reverseString(String s) { return new StringBuilder(s).reverse().toString(); }", "[\"hello\"]", "\"olleh\""));
        FunctionSignature cppSig = SolutionWrapperGeneratorTest.signature("reverseString", "String", "s", "String");
        assertExecuted(service.execute("C++", cppSig, "string reverseString(string s) { reverse(s.begin(), s.end()); return s; }", "[\"hello\"]", "\"olleh\""));
        FunctionSignature pySig = SolutionWrapperGeneratorTest.signature("reverse_string", "String", "s", "String");
        assertExecuted(service.execute("Python", pySig, "def reverse_string(s):\n    return s[::-1]", "[\"hello\"]", "\"olleh\""));
    }

    @Test void executesArrayFunction() {
        FunctionSignature sig = SolutionWrapperGeneratorTest.signature("doubleValues", "int[]", "values", "int[]");
        FunctionExecutionResult result = service.execute("Java", sig, "public int[] doubleValues(int[] values) { for(int i=0;i<values.length;i++) values[i]*=2; return values; }", "[[1,2,3]]", "[2,4,6]");
        assertEquals("EXECUTED", result.status(), result.errorMessage()); assertTrue(result.passed());
        assertEquals("[2,4,6]", result.normalizedOutput());
    }

    @Test void reportsCompilationAndRuntimeErrors() {
        FunctionSignature sig = SolutionWrapperGeneratorTest.signature("reverseString", "String", "s", "String");
        assertEquals("COMPILATION_ERROR", service.execute("Java", sig, "public String reverseString(String s) { invalid }", "[\"hello\"]", "\"olleh\"").status());
        assertEquals("COMPILATION_ERROR", service.execute("C++", sig, "string reverseString(string s) { invalid }", "[\"hello\"]", "\"olleh\"").status());
        FunctionSignature py = SolutionWrapperGeneratorTest.signature("reverse_string", "String", "s", "String");
        assertEquals("RUNTIME_ERROR", service.execute("Python", py, "def reverse_string(s):\n    raise RuntimeError('boom')", "[\"hello\"]", "\"olleh\"").status());
    }

    @Test void preservesLegacyStdinExecution() {
        String source = "public class Solution { public static void main(String[] a) throws Exception { System.out.print(new String(System.in.readAllBytes()).trim()); } }";
        ExecutionResult result = codeExecutionService.executeJava(source, "legacy input");
        assertEquals("EXECUTED", result.getStatus()); assertEquals("legacy input", result.getOutput());
    }

    private void assertExecuted(FunctionExecutionResult result) {
        assertEquals("EXECUTED", result.status(), result.errorMessage()); assertTrue(result.passed()); assertEquals("\"olleh\"", result.normalizedOutput());
    }
}
