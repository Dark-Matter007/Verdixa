package com.leetcode.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leetcode.backend.dto.*;
import com.leetcode.backend.execution.*;
import com.leetcode.backend.model.*;
import com.leetcode.backend.repository.ProblemRepository;
import org.springframework.stereotype.Service;
import java.util.*;

/** Non-persisting Run Code path. Deliberately has no SubmissionRepository dependency. */
@Service
public class RunCodeService {
    private static final int MAX_SOURCE_CHARS = 200_000;
    private static final int MAX_STDIN_CHARS = 64_000;
    private static final int MAX_CASES = 10;
    private static final int MAX_JSON_CASE_CHARS = 32_000;
    private final ProblemRepository problems; private final FunctionExecutionService functions;
    private final CodeExecutionService code; private final ObjectMapper mapper = new ObjectMapper();
    public RunCodeService(ProblemRepository problems, FunctionExecutionService functions, CodeExecutionService code) {
        this.problems=problems; this.functions=functions; this.code=code;
    }
    public RunCodeResponse run(RunCodeRequest request) {
        if (request == null || request.getProblemId() == null) throw new IllegalArgumentException("Problem is required.");
        if (request.getSourceCode() == null || request.getSourceCode().length() > MAX_SOURCE_CHARS) throw new IllegalArgumentException("Source code must be between 1 and 200000 characters.");
        if (request.getInput() != null && request.getInput().length() > MAX_STDIN_CHARS) throw new IllegalArgumentException("Custom input is too large.");
        Problem problem=problems.findWithFunctionSignatureById(request.getProblemId()).orElseThrow(() -> new IllegalArgumentException("Problem not found."));
        if (problem.getExecutionMode()==ExecutionMode.STDIN) {
            ExecutionResult r=execute(request.getLanguage(), request.getSourceCode(), request.getInput());
            return new RunCodeResponse("STDIN", List.of(new RunCaseResult(1,r.getStatus(),text(r.getOutput()),null,null,r.getExecutionTimeMs(),safe(r.getErrorMessage()))));
        }
        if (request.getTestCases()==null || request.getTestCases().isEmpty()) throw new IllegalArgumentException("At least one custom function case is required.");
        if (request.getTestCases().size() > MAX_CASES) throw new IllegalArgumentException("At most 10 custom function cases are allowed.");
        List<RunCaseResult> results=new ArrayList<>(); int number=0;
        for (RunCodeRequest.CustomRunCase c:request.getTestCases()) {
            number++; if(c.getArguments()==null) throw new IllegalArgumentException("Case " + number + " arguments must be a JSON array.");
            String arguments=json(c.getArguments()), expected=c.getExpected()==null?null:json(c.getExpected());
            if (!array(arguments)) throw new IllegalArgumentException("Case " + number + " arguments must be a JSON array.");
            if (arguments.length() > MAX_JSON_CASE_CHARS || (expected != null && expected.length() > MAX_JSON_CASE_CHARS)) throw new IllegalArgumentException("Case " + number + " is too large.");
            FunctionExecutionResult r=functions.execute(request.getLanguage(),problem.getFunctionSignature(),request.getSourceCode(),arguments,expected);
            Object actual=parse(r.normalizedOutput());
            results.add(new RunCaseResult(number,r.status(),actual,parse(expected),expected==null?null:r.passed(),r.executionTimeMs(),safe(r.errorMessage())));
            if (fatal(r.status())) break;
        }
        return new RunCodeResponse("FUNCTION",results);
    }
    private ExecutionResult execute(String language,String source,String input) {
        String l=language==null?"":language.toLowerCase(Locale.ROOT);
        if(l.equals("java")) return code.executeJava(source,input); if(l.equals("cpp")||l.equals("c++")) return code.executeCpp(source,input);
        if(l.equals("python")||l.equals("python3")) return code.executePython(source,input); throw new IllegalArgumentException("Unsupported language.");
    }
    private String json(Object value) { try { return mapper.writeValueAsString(value); } catch (Exception ex) { throw new IllegalArgumentException("Case value must be valid JSON."); } }
    private boolean array(String value) { try { return mapper.readTree(value).isArray(); } catch (Exception ex) { return false; } }
    private Object parse(String value) { try{return value==null||value.isBlank()?null:mapper.readValue(value, Object.class);}catch(Exception e){return null;} }
    private String text(String value) { return value==null?null:value; }
    private boolean fatal(String s){return Set.of("COMPILATION_ERROR","RUNTIME_ERROR","TIME_LIMIT_EXCEEDED","EXECUTION_ERROR").contains(s);}
    private String safe(String value){return value==null||value.isBlank()?null:value;}
}
