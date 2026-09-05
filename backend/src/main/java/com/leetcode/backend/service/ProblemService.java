package com.leetcode.backend.service;

import com.leetcode.backend.model.Problem;
import com.leetcode.backend.dto.ProblemDetailResponse;
import com.leetcode.backend.repository.ProblemRepository;
import com.leetcode.backend.repository.TestCaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
public class ProblemService {

    private final ProblemRepository problemRepository;
    private final TestCaseRepository testCaseRepository;

    public ProblemService(ProblemRepository problemRepository, TestCaseRepository testCaseRepository) {
        this.problemRepository = problemRepository;
        this.testCaseRepository = testCaseRepository;
    }

    public List<Problem> getAllProblems() {
        return problemRepository.findByActiveTrue();
    }

    /** Administrative listing intentionally includes inactive (soft-deleted) problems. */
    @Transactional(readOnly = true)
    public List<Problem> getAllProblemsForAdmin() {
        var problems = problemRepository.findAll();
        problems.forEach(problem -> { if (problem.getFunctionSignature() != null) problem.getFunctionSignature().getParameters().size(); });
        return problems;
    }

    @Transactional(readOnly = true)
    public Problem getProblemById(Long id) {
        Problem problem = problemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Problem not found with id: " + id));
        if (problem.getFunctionSignature() != null) problem.getFunctionSignature().getParameters().size();
        return problem;
    }

    public Problem getActiveProblemById(Long id) {
        Problem problem = getProblemById(id);
        if (!problem.isActive()) {
            throw new RuntimeException("Problem not found with id: " + id);
        }
        return problem;
    }

    /** Materialize solver data inside the persistence transaction; never serialize a managed entity. */
    @Transactional(readOnly = true)
    public ProblemDetailResponse getProblemDetail(Long id, boolean includeInactive) {
        Problem problem = problemRepository.findWithFunctionSignatureById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Problem not found."));
        if (!includeInactive && !problem.isActive()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Problem not found.");
        }
        return ProblemDetailResponse.from(problem);
    }

    public Problem createProblem(Problem problem) {
        validateProblem(problem);
        if (problem.isActive()) {
            throw new IllegalArgumentException("Create the problem as a draft, add exactly four hidden tests, then publish it.");
        }
        return problemRepository.save(problem);
    }

    @Transactional
    public ProblemDetailResponse updateProblem(Long id, Problem updatedProblem) {
        validateProblem(updatedProblem);
        Problem existingProblem = problemRepository.findWithFunctionSignatureById(id)
                .orElseThrow(() -> new RuntimeException("Problem not found with id: " + id));

        existingProblem.setTitle(updatedProblem.getTitle());
        existingProblem.setDescription(updatedProblem.getDescription());
        existingProblem.setDifficulty(updatedProblem.getDifficulty());
        existingProblem.setConstraints(updatedProblem.getConstraints());
        existingProblem.setInputFormat(updatedProblem.getInputFormat());
        existingProblem.setOutputFormat(updatedProblem.getOutputFormat());
        existingProblem.setExamples(updatedProblem.getExamples());
        existingProblem.setTags(updatedProblem.getTags());
        existingProblem.setStarterCode(updatedProblem.getStarterCode());
        existingProblem.setExecutionMode(updatedProblem.getExecutionMode());
        mergeFunctionSignature(existingProblem, updatedProblem);
        if (!existingProblem.isActive() && updatedProblem.isActive()) validatePublishable(id);
        existingProblem.setActive(updatedProblem.isActive());

        problemRepository.save(existingProblem);
        problemRepository.flush();
        return ProblemDetailResponse.from(existingProblem);
    }

    public void deleteProblem(Long id) {
        Problem problem = getProblemById(id);
        problem.setActive(false);
        problemRepository.save(problem);
    }

    public List<Problem> searchProblems(String title) {
        return problemRepository.findByTitleContainingIgnoreCase(title)
                .stream().filter(Problem::isActive).toList();
    }

    public List<Problem> getProblemsByDifficulty(String difficulty) {
        return problemRepository.findByDifficultyIgnoreCase(difficulty)
                .stream().filter(Problem::isActive).toList();
    }

    private void validateProblem(Problem problem) {
        if (problem == null || problem.getTitle() == null || problem.getTitle().isBlank()
                || problem.getDescription() == null || problem.getDescription().isBlank()) {
            throw new IllegalArgumentException("Title and description are required.");
        }
        String difficulty = problem.getDifficulty();
        if (difficulty == null || !(difficulty.toUpperCase(Locale.ROOT).equals("EASY")
                || difficulty.toUpperCase(Locale.ROOT).equals("MEDIUM")
                || difficulty.toUpperCase(Locale.ROOT).equals("HARD"))) {
            throw new IllegalArgumentException("Difficulty must be EASY, MEDIUM, or HARD.");
        }
        if (problem.getExecutionMode() == com.leetcode.backend.model.ExecutionMode.FUNCTION) {
            var signature = problem.getFunctionSignature();
            if (signature == null || signature.getFunctionName() == null || !signature.getFunctionName().matches("[A-Za-z_][A-Za-z0-9_]*")
                    || signature.getReturnType() == null || signature.getReturnType().isBlank()) {
                throw new IllegalArgumentException("FUNCTION problems require a valid function name and return type.");
            }
            if (signature.getParameters() == null) throw new IllegalArgumentException("FUNCTION parameters must be configured.");
            for (var parameter : signature.getParameters()) if (parameter.getName() == null || !parameter.getName().matches("[A-Za-z_][A-Za-z0-9_]*") || parameter.getType() == null || parameter.getType().isBlank()) throw new IllegalArgumentException("FUNCTION parameter names and types must be valid.");
        }
    }

    private void validatePublishable(Long problemId) {
        if (testCaseRepository.countByProblemIdAndHiddenTrue(problemId) != 4) {
            throw new IllegalArgumentException("A published problem requires exactly 4 hidden official test cases.");
        }
    }

    private void mergeFunctionSignature(Problem existing, Problem update) {
        if (update.getExecutionMode() != com.leetcode.backend.model.ExecutionMode.FUNCTION) {
            existing.setFunctionSignature(null);
            return;
        }
        var incoming = update.getFunctionSignature();
        var signature = existing.getFunctionSignature();
        if (signature == null) {
            signature = new com.leetcode.backend.model.FunctionSignature();
            existing.setFunctionSignature(signature);
        }
        signature.setFunctionName(incoming.getFunctionName());
        signature.setReturnType(incoming.getReturnType());
        var current = signature.getParameters();
        var requested = incoming.getParameters();
        for (int index = 0; index < requested.size(); index++) {
            com.leetcode.backend.model.FunctionParameter target;
            if (index < current.size()) target = current.get(index);
            else {
                target = new com.leetcode.backend.model.FunctionParameter();
                current.add(target);
            }
            target.setName(requested.get(index).getName());
            target.setType(requested.get(index).getType());
            target.setParameterOrder(index);
        }
        while (current.size() > requested.size()) current.remove(current.size() - 1);
    }
}
