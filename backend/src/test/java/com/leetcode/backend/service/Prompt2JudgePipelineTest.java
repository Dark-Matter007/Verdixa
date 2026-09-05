package com.leetcode.backend.service;

import com.leetcode.backend.dto.*;
import com.leetcode.backend.execution.*;
import com.leetcode.backend.model.*;
import com.leetcode.backend.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class Prompt2JudgePipelineTest {
    private FunctionValueCodec codec; private CodeExecutionService code; private FunctionExecutionService functions;
    private Problem problem; private List<TestCase> official;

    @BeforeEach void setup() {
        codec=new FunctionValueCodec(); code=new CodeExecutionService();
        ReflectionTestUtils.setField(code,"javacCommand","javac"); ReflectionTestUtils.setField(code,"javaCommand","java");
        ReflectionTestUtils.setField(code,"gxxCommand",windows()?"C:\\msys64\\ucrt64\\bin\\g++.exe":"g++"); ReflectionTestUtils.setField(code,"pythonCommand",windows()?"python":"python3");
        ReflectionTestUtils.setField(code,"runtimePath",windows()?"C:\\msys64\\ucrt64\\bin;C:\\msys64\\usr\\bin":"");
        functions=new FunctionExecutionService(codec,code,List.of(new JavaSolutionWrapperGenerator(codec),new CppSolutionWrapperGenerator(codec),new PythonSolutionWrapperGenerator(codec)));
        problem=problem(); official=List.of(test("hello",false),test("Verdixa",false),test("",true),test("x",true),test("aaaa",true),test("boundary value",true));
    }

    @Test void functionRunSupportsThreeLanguagesMultipleCasesOptionalExpectedAndNeverPersists() {
        ProblemRepository problems=mock(ProblemRepository.class); when(problems.findWithFunctionSignatureById(2L)).thenReturn(Optional.of(problem));
        SubmissionRepository submissions=mock(SubmissionRepository.class); when(submissions.count()).thenReturn(7L);
        RunCodeService service=new RunCodeService(problems,functions,code);
        long before=submissions.count();
        for (String language:List.of("java","cpp","python")) {
            RunCodeRequest request=new RunCodeRequest(); request.setProblemId(2L); request.setLanguage(language); request.setSourceCode(source(language));
            RunCodeRequest.CustomRunCase first=custom("hello","olleh"), second=custom("Verdixa",null); request.setTestCases(List.of(first,second));
            RunCodeResponse response=service.run(request);
            assertEquals(2,response.testCases().size()); assertTrue(response.testCases().get(0).passed());
            assertEquals("olleh",response.testCases().get(0).actual()); assertNull(response.testCases().get(1).passed());
        }
        assertEquals(before,submissions.count()); verify(submissions,times(2)).count(); verifyNoMoreInteractions(submissions);
    }

    @Test void functionSubmitAcceptsJavaCppAndPythonAndPersistsOneEach() {
        for(String language:List.of("java","cpp","python")) {
            Fixture f=fixture(); Submission result=f.service.createSubmission(1L,2L,language,source(language));
            assertEquals("ACCEPTED",result.getStatus()); assertEquals(6,result.getPassedTestCases()); assertEquals(6,result.getTotalTestCases());
            assertEquals(4,result.getTestCaseResults().stream().filter(JudgeTestCaseResult::hidden).count());
            verify(f.submissions,atLeast(2)).save(any(Submission.class));
        }
    }

    @Test void wrongAnswerContinuesAndHiddenResultsRevealNoValues() {
        Fixture f=fixture(); Submission result=f.service.createSubmission(1L,2L,"java","public String reverseString(String s) { return s; }");
        assertEquals("WRONG_ANSWER",result.getStatus()); assertTrue(result.getPassedTestCases()<6);
        result.getTestCaseResults().stream().filter(JudgeTestCaseResult::hidden).forEach(r -> {
            assertNull(r.arguments()); assertNull(r.input()); assertNull(r.expected()); assertNull(r.actual());
            assertFalse(r.message().contains("boundary value"));
        });
        assertFalse(result.getErrorMessage().contains("hello"));
        try {
            String payload=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(new SubmissionResponse(result));
            assertFalse(payload.contains("boundary value")); assertFalse(payload.contains("functionArguments"));
        } catch (Exception e) { fail(e); }
    }

    @Test void stdinRunAndSubmitRemainOperational() {
        problem.setExecutionMode(ExecutionMode.STDIN); TestCase tc=new TestCase(); tc.setInputData("legacy"); tc.setExpectedOutput("legacy"); tc.setHidden(false); official=List.of(tc);
        ProblemRepository problems=mock(ProblemRepository.class); when(problems.findWithFunctionSignatureById(2L)).thenReturn(Optional.of(problem));
        RunCodeRequest request=new RunCodeRequest(); request.setProblemId(2L); request.setLanguage("java"); request.setInput("legacy");
        request.setSourceCode("public class Solution { public static void main(String[] a)throws Exception{System.out.print(new String(System.in.readAllBytes()).trim());}}");
        assertEquals("EXECUTED",new RunCodeService(problems,functions,code).run(request).testCases().get(0).status());
        Fixture f=fixture(); Submission submitted=f.service.createSubmission(1L,2L,"java",request.getSourceCode());
        assertEquals("ACCEPTED",submitted.getStatus()); verify(f.submissions,atLeast(2)).save(any());
    }

    private Fixture fixture() {
        SubmissionRepository submissions=mock(SubmissionRepository.class); UserRepository users=mock(UserRepository.class);
        ProblemRepository problems=mock(ProblemRepository.class); TestCaseRepository tests=mock(TestCaseRepository.class);
        when(users.findById(1L)).thenReturn(Optional.of(new User())); when(problems.findWithFunctionSignatureById(2L)).thenReturn(Optional.of(problem)); when(tests.findByProblemId(2L)).thenReturn(official);
        when(submissions.save(any())).thenAnswer(i->i.getArgument(0));
        ContestRepository contests=mock(ContestRepository.class); ContestProblemRepository contestProblems=mock(ContestProblemRepository.class); ContestRegistrationRepository registrations=mock(ContestRegistrationRepository.class);
        return new Fixture(new SubmissionService(submissions,users,problems,tests,code,functions,contests,contestProblems,registrations),submissions);
    }
    private record Fixture(SubmissionService service,SubmissionRepository submissions) {}
    private Problem problem(){ Problem p=new Problem(); p.setTitle("Reverse String"); p.setDescription("Reverse"); p.setDifficulty("EASY"); p.setExecutionMode(ExecutionMode.FUNCTION); p.setActive(true);
        FunctionSignature s=new FunctionSignature(); s.setFunctionName("reverseString"); s.setReturnType("String"); FunctionParameter fp=new FunctionParameter(); fp.setName("s"); fp.setType("String"); s.setParameters(List.of(fp)); p.setFunctionSignature(s); return p; }
    private TestCase test(String input,boolean hidden){TestCase t=new TestCase();t.setInputData("");t.setFunctionArguments("["+quote(input)+"]");t.setExpectedOutput(quote(new StringBuilder(input).reverse().toString()));t.setHidden(hidden);return t;}
    private RunCodeRequest.CustomRunCase custom(String input,String expected){return customFixed(input,expected);}
    private RunCodeRequest.CustomRunCase customFixed(String input,String expected){try{var mapper=new com.fasterxml.jackson.databind.ObjectMapper();RunCodeRequest.CustomRunCase c=new RunCodeRequest.CustomRunCase();c.setArguments(mapper.readTree("["+quote(input)+"]"));if(expected!=null)c.setExpected(mapper.readTree(quote(expected)));return c;}catch(Exception e){throw new RuntimeException(e);}}
    private String quote(String s){try{return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(s);}catch(Exception e){throw new RuntimeException(e);}}
    private String source(String l){return switch(l){case "java"->"public String reverseString(String s) { return new StringBuilder(s).reverse().toString(); }";case "cpp"->"string reverseString(string s) { reverse(s.begin(), s.end()); return s; }";default->"def reverse_string(s):\n    return s[::-1]";};}
    private boolean windows(){return System.getProperty("os.name").toLowerCase().contains("win");}
}
