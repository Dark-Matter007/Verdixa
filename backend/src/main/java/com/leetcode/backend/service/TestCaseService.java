package com.leetcode.backend.service;

import com.leetcode.backend.model.Problem;
import com.leetcode.backend.model.TestCase;
import com.leetcode.backend.model.ExecutionMode;
import com.leetcode.backend.execution.FunctionValueCodec;
import com.leetcode.backend.repository.ProblemRepository;
import com.leetcode.backend.repository.TestCaseRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TestCaseService {

    private final TestCaseRepository testCaseRepository;
    private final ProblemRepository problemRepository;
    private final FunctionValueCodec functionValueCodec;

    public TestCaseService(
            TestCaseRepository testCaseRepository,
            ProblemRepository problemRepository,
            FunctionValueCodec functionValueCodec) {

        this.testCaseRepository = testCaseRepository;
        this.problemRepository = problemRepository;
        this.functionValueCodec = functionValueCodec;
    }

    public List<TestCase> getPublicTestCases(Long problemId) {

        verifyProblemExists(problemId);

        return testCaseRepository.findByProblemIdAndHiddenFalse(problemId);
    }

    public List<TestCase> getAllTestCases(Long problemId) {

        verifyProblemExists(problemId);

        return testCaseRepository.findByProblemId(problemId);
    }

    public TestCase createTestCase(
            Long problemId,
            TestCase testCase) {

        Problem problem = verifyProblemExists(problemId);
        validateTestCase(problem, testCase);
        if (testCase.isHidden() && testCaseRepository.countByProblemIdAndHiddenTrue(problemId) >= 4) {
            throw new IllegalArgumentException("A problem may have exactly four hidden official test cases.");
        }

        testCase.setProblem(problem);

        return testCaseRepository.save(testCase);
    }

    public TestCase updateTestCase(
            Long problemId,
            Long testCaseId,
            TestCase updatedTestCase) {

        TestCase existingTestCase = getTestCase(testCaseId);
        if (!existingTestCase.getProblem().getId().equals(problemId)) {
            throw new IllegalArgumentException("Test case does not belong to this problem.");
        }
        validateTestCase(existingTestCase.getProblem(), updatedTestCase);
        if (existingTestCase.getProblem().isActive() && existingTestCase.isHidden() && !updatedTestCase.isHidden()) {
            throw new IllegalArgumentException("A published problem must retain exactly four hidden official test cases.");
        }
        boolean wasHidden = existingTestCase.isHidden();
        if (updatedTestCase.isHidden() && !wasHidden
                && testCaseRepository.countByProblemIdAndHiddenTrue(problemId) >= 4) {
            throw new IllegalArgumentException("A problem may have exactly four hidden official test cases.");
        }

        existingTestCase.setInputData(
                updatedTestCase.getInputData()
        );

        existingTestCase.setExpectedOutput(
                updatedTestCase.getExpectedOutput()
        );

        existingTestCase.setFunctionArguments(updatedTestCase.getFunctionArguments());

        existingTestCase.setHidden(updatedTestCase.isHidden());

        return testCaseRepository.save(existingTestCase);
    }

    public void deleteTestCase(Long problemId, Long testCaseId) {

        TestCase existingTestCase = getTestCase(testCaseId);
        if (!existingTestCase.getProblem().getId().equals(problemId)) {
            throw new IllegalArgumentException("Test case does not belong to this problem.");
        }
        if (existingTestCase.getProblem().isActive() && existingTestCase.isHidden()) {
            throw new IllegalArgumentException("A hidden test cannot be deleted while the problem is published.");
        }
        testCaseRepository.delete(existingTestCase);
    }

    private TestCase getTestCase(Long testCaseId) {

        return testCaseRepository.findById(testCaseId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Test case not found with id: "
                                        + testCaseId
                        )
                );
    }

    private Problem verifyProblemExists(Long problemId) {

        return problemRepository.findById(problemId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Problem not found with id: "
                                        + problemId
                        )
                );
    }

    private void validateTestCase(Problem problem, TestCase testCase) {
        if (testCase == null || testCase.getExpectedOutput() == null) throw new IllegalArgumentException("Test case expected output is required.");
        if (problem.getExecutionMode() == ExecutionMode.FUNCTION) {
            functionValueCodec.validateSignature(problem.getFunctionSignature());
            functionValueCodec.parseArguments(testCase.getFunctionArguments(), problem.getFunctionSignature().getParameters());
            functionValueCodec.parse(testCase.getExpectedOutput(), problem.getFunctionSignature().getReturnType());
            if (testCase.getInputData() == null) testCase.setInputData("");
        } else {
            if (testCase.getInputData() == null) throw new IllegalArgumentException("Test case input is required.");
        }
    }
}
