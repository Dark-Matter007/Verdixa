package com.leetcode.backend.service;

import com.leetcode.backend.execution.CodeExecutionService;
import com.leetcode.backend.execution.ExecutionResult;
import com.leetcode.backend.execution.FunctionExecutionService;
import com.leetcode.backend.execution.FunctionExecutionResult;
import com.leetcode.backend.dto.JudgeTestCaseResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leetcode.backend.model.ExecutionMode;
import com.leetcode.backend.model.Problem;
import com.leetcode.backend.model.Submission;
import com.leetcode.backend.model.TestCase;
import com.leetcode.backend.model.User;
import com.leetcode.backend.repository.ProblemRepository;
import com.leetcode.backend.repository.SubmissionRepository;
import com.leetcode.backend.repository.TestCaseRepository;
import com.leetcode.backend.repository.UserRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final ProblemRepository problemRepository;
    private final TestCaseRepository testCaseRepository;
    private final CodeExecutionService codeExecutionService;
    private final FunctionExecutionService functionExecutionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SubmissionService(
            SubmissionRepository submissionRepository,
            UserRepository userRepository,
            ProblemRepository problemRepository,
            TestCaseRepository testCaseRepository,
            CodeExecutionService codeExecutionService,
            FunctionExecutionService functionExecutionService) {

        this.submissionRepository = submissionRepository;
        this.userRepository = userRepository;
        this.problemRepository = problemRepository;
        this.testCaseRepository = testCaseRepository;
        this.codeExecutionService = codeExecutionService;
        this.functionExecutionService = functionExecutionService;
    }

    // =========================================================
    // CREATE SUBMISSION
    // =========================================================

    public Submission createSubmission(
            Long userId,
            Long problemId,
            String language,
            String sourceCode) {

        // -----------------------------------------------------
        // FIND USER
        // -----------------------------------------------------

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found with id: " + userId
                        )
                );

        // -----------------------------------------------------
        // FIND PROBLEM
        // -----------------------------------------------------

        Problem problem = problemRepository.findWithFunctionSignatureById(problemId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Problem not found with id: " + problemId
                        )
                );

        // -----------------------------------------------------
        // FIND TEST CASES
        // -----------------------------------------------------

        List<TestCase> testCases =
                testCaseRepository.findByProblemId(problemId);

        // -----------------------------------------------------
        // CREATE SUBMISSION
        // -----------------------------------------------------

        Submission submission = new Submission();

        submission.setUser(user);
        submission.setProblem(problem);
        submission.setLanguage(language);
        submission.setSourceCode(sourceCode);

        submission.setStatus("RUNNING");
        submission.setOutput("");
        submission.setErrorMessage("");
        submission.setExecutionTimeMs(0L);
        submission.setPassedTestCases(0);
        submission.setTotalTestCases(testCases.size());

        Submission savedSubmission =
                submissionRepository.save(submission);

        int passed = 0;
        long totalExecutionTime = 0;

        try {

            // =================================================
            // NO TEST CASES
            // =================================================

            if (testCases.isEmpty()) {

                savedSubmission.setStatus(
                        "NO_TEST_CASES"
                );

                savedSubmission.setErrorMessage(
                        "This problem has no test cases."
                );

                return submissionRepository.save(
                        savedSubmission
                );
            }

            // =================================================
            // VALIDATE LANGUAGE
            // =================================================

            String normalizedLanguage =
                    normalizeLanguage(language);

            if (normalizedLanguage == null) {

                savedSubmission.setStatus(
                        "LANGUAGE_NOT_SUPPORTED"
                );

                savedSubmission.setErrorMessage(
                        "Supported languages are Java, C++, and Python."
                );

                return submissionRepository.save(
                        savedSubmission
                );
            }

            if (problem.getExecutionMode() == ExecutionMode.FUNCTION) {
                return executeFunctionSubmission(savedSubmission, problem, testCases, normalizedLanguage, sourceCode);
            }

            // =================================================
            // RUN EVERY TEST CASE
            // =================================================

            for (TestCase testCase : testCases) {

                ExecutionResult result;

                // -------------------------------------------------
                // JAVA
                // -------------------------------------------------

                if (normalizedLanguage.equals("java")) {

                    result =
                            codeExecutionService.executeJava(
                                    sourceCode,
                                    testCase.getInputData()
                            );
                }

                // -------------------------------------------------
                // C++
                // -------------------------------------------------

                else if (normalizedLanguage.equals("cpp")) {

                    result =
                            codeExecutionService.executeCpp(
                                    sourceCode,
                                    testCase.getInputData()
                            );
                }

                // -------------------------------------------------
                // PYTHON
                // -------------------------------------------------

                else {

                    result =
                            codeExecutionService.executePython(
                                    sourceCode,
                                    testCase.getInputData()
                            );
                }

                // -------------------------------------------------
                // EXECUTION TIME
                // -------------------------------------------------

                totalExecutionTime +=
                        result.getExecutionTimeMs();

                // =================================================
                // COMPILATION / RUNTIME / EXECUTION ERROR
                // =================================================

                if (!"EXECUTED".equals(
                        result.getStatus())) {

                    savedSubmission.setStatus(
                            result.getStatus()
                    );

                    savedSubmission.setOutput(
                            result.getOutput()
                    );

                    savedSubmission.setErrorMessage(
                            result.getErrorMessage()
                    );

                    savedSubmission.setPassedTestCases(
                            passed
                    );

                    savedSubmission.setTotalTestCases(
                            testCases.size()
                    );

                    savedSubmission.setExecutionTimeMs(
                            totalExecutionTime
                    );

                    return submissionRepository.save(
                            savedSubmission
                    );
                }

                // =================================================
                // NORMALIZE OUTPUT
                // =================================================

                String actualOutput =
                        normalizeOutput(
                                result.getOutput()
                        );

                String expectedOutput =
                        normalizeOutput(
                                testCase.getExpectedOutput()
                        );

                // =================================================
                // TEST PASSED
                // =================================================

                if (actualOutput.equals(
                        expectedOutput)) {

                    passed++;

                    savedSubmission.setPassedTestCases(
                            passed
                    );
                }

                // =================================================
                // TEST FAILED
                // =================================================

                else {

                    savedSubmission.setStatus(
                            "WRONG_ANSWER"
                    );

                    savedSubmission.setOutput(
                            result.getOutput()
                    );

                    savedSubmission.setErrorMessage(
                            "Test case failed. Expected: "
                                    + testCase.getExpectedOutput()
                                    + " | Actual: "
                                    + result.getOutput()
                    );

                    savedSubmission.setPassedTestCases(
                            passed
                    );

                    savedSubmission.setTotalTestCases(
                            testCases.size()
                    );

                    savedSubmission.setExecutionTimeMs(
                            totalExecutionTime
                    );

                    return submissionRepository.save(
                            savedSubmission
                    );
                }
            }

            // =================================================
            // ALL TEST CASES PASSED
            // =================================================

            savedSubmission.setStatus(
                    "ACCEPTED"
            );

            savedSubmission.setOutput(
                    "All test cases passed."
            );

            savedSubmission.setErrorMessage("");

            savedSubmission.setPassedTestCases(
                    passed
            );

            savedSubmission.setTotalTestCases(
                    testCases.size()
            );

            savedSubmission.setExecutionTimeMs(
                    totalExecutionTime
            );

        } catch (Exception e) {

            savedSubmission.setStatus(
                    "EXECUTION_ERROR"
            );

            savedSubmission.setOutput("");

            savedSubmission.setErrorMessage(
                    e.getMessage() != null
                            ? e.getMessage()
                            : "Unknown execution error."
            );

            savedSubmission.setPassedTestCases(
                    passed
            );

            savedSubmission.setTotalTestCases(
                    testCases.size()
            );

            savedSubmission.setExecutionTimeMs(
                    totalExecutionTime
            );
        }

        return submissionRepository.save(
                savedSubmission
        );
    }

    private Submission executeFunctionSubmission(Submission submission, Problem problem, List<TestCase> testCases,
                                                 String language, String sourceCode) {
        long hiddenCount = testCases.stream().filter(TestCase::isHidden).count();
        if (hiddenCount != 4) {
            submission.setStatus("EXECUTION_ERROR");
            submission.setErrorMessage("Published FUNCTION problem is not configured with exactly four hidden tests.");
            return submissionRepository.save(submission);
        }
        int passed=0, testNumber=0, hiddenNumber=0; long runtime=0; boolean wrong=false;
        java.util.ArrayList<JudgeTestCaseResult> details=new java.util.ArrayList<>();
        for (TestCase testCase:testCases) {
            testNumber++; if(testCase.isHidden()) hiddenNumber++;
            FunctionExecutionResult result=functionExecutionService.execute(language,problem.getFunctionSignature(),sourceCode,
                    testCase.getFunctionArguments(),testCase.getExpectedOutput());
            runtime+=result.executionTimeMs();
            if(result.passed()) passed++; else if("WRONG_ANSWER".equals(result.status())) wrong=true;
            if(testCase.isHidden()) {
                details.add(JudgeTestCaseResult.hidden(hiddenNumber,result.status(),result.executionTimeMs()));
            } else {
                details.add(new JudgeTestCaseResult(testNumber,false,json(testCase.getFunctionArguments()),null,
                        json(testCase.getExpectedOutput()),json(result.normalizedOutput()),result.status(),result.executionTimeMs(),
                        result.errorMessage().isBlank()?null:result.errorMessage()));
            }
            if (isFatal(result.status())) {
                submission.setStatus(result.status());
                submission.setErrorMessage(testCase.isHidden()?"Execution failed while evaluating a hidden test.":safeError(result.errorMessage()));
                break;
            }
        }
        if (!isFatal(submission.getStatus())) {
            submission.setStatus(wrong?"WRONG_ANSWER":"ACCEPTED");
            submission.setErrorMessage(wrong?"One or more official tests failed.":"");
        }
        submission.setPassedTestCases(passed); submission.setTotalTestCases(testCases.size()); submission.setExecutionTimeMs(runtime);
        submission.setOutput("ACCEPTED".equals(submission.getStatus())?"All official tests passed.":"Judge completed.");
        Submission saved=submissionRepository.save(submission); saved.setTestCaseResults(details); return saved;
    }

    private boolean isFatal(String status) {
        return java.util.Set.of("COMPILATION_ERROR","RUNTIME_ERROR","TIME_LIMIT_EXCEEDED","EXECUTION_ERROR").contains(status);
    }
    private String safeError(String error) { return error==null||error.isBlank()?"Execution failed.":error; }
    private Object json(String value) { try{return value==null||value.isBlank()?null:objectMapper.readValue(value, Object.class);}catch(Exception e){return null;} }

    // =========================================================
    // NORMALIZE LANGUAGE
    // =========================================================

    private String normalizeLanguage(
            String language) {

        if (language == null) {
            return null;
        }

        String value =
                language.trim()
                        .toLowerCase();

        switch (value) {

            case "java":
                return "java";

            case "c++":
            case "cpp":
            case "c plus plus":
                return "cpp";

            case "python":
            case "python3":
                return "python";

            default:
                return null;
        }
    }

    // =========================================================
    // NORMALIZE OUTPUT
    // =========================================================

    private String normalizeOutput(
            String output) {

        if (output == null) {
            return "";
        }

        return output
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .trim();
    }

    // =========================================================
    // GET SUBMISSION
    // =========================================================

    public Submission getSubmissionById(
            Long id) {

        return submissionRepository.findWithUserAndProblemById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Submission not found with id: "
                                        + id
                        )
                );
    }

    // =========================================================
    // USER SUBMISSIONS
    // =========================================================

    public List<Submission> getUserSubmissions(
            Long userId) {

        return submissionRepository
                .findByUserIdOrderBySubmittedAtDesc(
                        userId
                );
    }

    // =========================================================
    // PROBLEM SUBMISSIONS
    // =========================================================

    public List<Submission> getProblemSubmissions(
            Long problemId) {

        return submissionRepository
                .findByProblemIdOrderBySubmittedAtDesc(
                        problemId
                );
    }

    // =========================================================
    // USER + PROBLEM SUBMISSIONS
    // =========================================================

    public List<Submission> getUserProblemSubmissions(
            Long userId,
            Long problemId) {

        return submissionRepository
                .findByUserIdAndProblemIdOrderBySubmittedAtDesc(
                        userId,
                        problemId
                );
    }
}
