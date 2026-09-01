package com.leetcode.backend.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.leetcode.backend.model.ExecutionMode;
import com.leetcode.backend.model.FunctionParameter;
import com.leetcode.backend.model.FunctionSignature;
import com.leetcode.backend.model.Problem;
import com.leetcode.backend.model.TestCase;
import com.leetcode.backend.repository.ProblemRepository;
import com.leetcode.backend.repository.TestCaseRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Idempotent repair for the named legacy FUNCTION fixtures. */
@Configuration
public class LegacyProblemMetadataRepair {
    private static final String REVERSE_STARTERS = "{\"java\":\"public String reverseString(String s) {\\n    return new StringBuilder(s).reverse().toString();\\n}\",\"cpp\":\"string reverseString(string s) {\\n    reverse(s.begin(), s.end());\\n    return s;\\n}\",\"python\":\"def reverse_string(s):\\n    return s[::-1]\"}";
    private static final String TWO_SUM_STARTERS = "{\"java\":\"public int[] twoSum(int[] nums, int target) {\\n    java.util.Map<Integer, Integer> seen = new java.util.HashMap<>();\\n    for (int i = 0; i < nums.length; i++) {\\n        int need = target - nums[i];\\n        if (seen.containsKey(need)) return new int[]{seen.get(need), i};\\n        seen.put(nums[i], i);\\n    }\\n    return new int[0];\\n}\",\"cpp\":\"vector<int> twoSum(vector<int> nums, int target) {\\n    unordered_map<int, int> seen;\\n    for (int i = 0; i < (int)nums.size(); ++i) {\\n        int need = target - nums[i];\\n        if (seen.count(need)) return {seen[need], i};\\n        seen[nums[i]] = i;\\n    }\\n    return {};\\n}\",\"python\":\"def two_sum(nums, target):\\n    seen = {}\\n    for i, value in enumerate(nums):\\n        need = target - value\\n        if need in seen:\\n            return [seen[need], i]\\n        seen[value] = i\\n    return []\"}";
    private static final String PARENTHESES_STARTERS = "{\"java\":\"public boolean isValid(String s) {\\n    java.util.Deque<Character> stack = new java.util.ArrayDeque<>();\\n    for (char c : s.toCharArray()) {\\n        if (c == '(' || c == '[' || c == '{') stack.push(c);\\n        else {\\n            if (stack.isEmpty()) return false;\\n            char open = stack.pop();\\n            if ((c == ')' && open != '(') || (c == ']' && open != '[') || (c == '}' && open != '{')) return false;\\n        }\\n    }\\n    return stack.isEmpty();\\n}\",\"cpp\":\"bool isValid(string s) {\\n    stack<char> st;\\n    for (char c : s) {\\n        if (c == '(' || c == '[' || c == '{') st.push(c);\\n        else {\\n            if (st.empty()) return false;\\n            char open = st.top(); st.pop();\\n            if ((c == ')' && open != '(') || (c == ']' && open != '[') || (c == '}' && open != '{')) return false;\\n        }\\n    }\\n    return st.empty();\\n}\",\"python\":\"def is_valid(s):\\n    pairs = {')': '(', ']': '[', '}': '{'}\\n    stack = []\\n    for char in s:\\n        if char in '([{':\\n            stack.append(char)\\n        elif not stack or stack.pop() != pairs.get(char):\\n            return False\\n    return not stack\"}";

    private final ProblemRepository problems;
    private final TestCaseRepository tests;
    private final ObjectMapper mapper = new ObjectMapper();
    private final TransactionTemplate transaction;

    public LegacyProblemMetadataRepair(ProblemRepository problems, TestCaseRepository tests, PlatformTransactionManager transactionManager) {
        this.problems = problems; this.tests = tests; this.transaction = new TransactionTemplate(transactionManager);
    }

    @Bean CommandLineRunner repairLegacyFunctionFixtures() { return args -> transaction.executeWithoutResult(status -> repair()); }

    public void repair() {
        problems.findAll().forEach(problem -> {
            if (!problem.isActive()) return;
            String title = problem.getTitle() == null ? "" : problem.getTitle().trim();
            if ("Reverse String".equalsIgnoreCase(title)) repairReverseString(problem);
            else if ("Two Sum Updated".equalsIgnoreCase(title)) repairTwoSum(problem);
            else if ("Valid Parentheses".equalsIgnoreCase(title)) repairValidParentheses(problem);
            else if ("P1787938966101".equals(title) && "d".equals(problem.getDescription())
                    && problem.getFunctionSignature() == null && (problem.getStarterCode() == null || problem.getStarterCode().isBlank())) {
                // Preserve the corrupt legacy row/history, but never publish a problem with no executable contract.
                problem.setActive(false); problems.save(problem);
            }
        });
    }

    private void repairReverseString(Problem problem) {
        configure(problem, "reverseString", "String", new String[][]{{"s", "String"}}, REVERSE_STARTERS);
        addMissingHidden(problem, new String[][]{{"[\"\"]", "\"\""}, {"[\"a\"]", "\"a\""}, {"[\"racecar\"]", "\"racecar\""}});
    }

    private void repairTwoSum(Problem problem) {
        configure(problem, "twoSum", "int[]", new String[][]{{"nums", "int[]"}, {"target", "int"}}, TWO_SUM_STARTERS);
        if (tests.findByProblemId(problem.getId()).isEmpty()) {
            add(problem, "[[2,7,11,15],9]", "[0,1]", false); add(problem, "[[3,2,4],6]", "[1,2]", false);
            addMissingHidden(problem, new String[][]{{"[[3,3],6]", "[0,1]"}, {"[[0,4,3,0],0]", "[0,3]"}, {"[[-1,-2,-3,-4,-5],-8]", "[2,4]"}, {"[[1,5,1,5],10]", "[1,3]"}});
        }
    }

    private void repairValidParentheses(Problem problem) {
        configure(problem, "isValid", "boolean", new String[][]{{"s", "String"}}, PARENTHESES_STARTERS);
        if (tests.findByProblemId(problem.getId()).isEmpty()) {
            add(problem, "[\"()\"]", "true", false); add(problem, "[\"()[]{}\"]", "true", false);
            addMissingHidden(problem, new String[][]{{"[\"(]\"]", "false"}, {"[\"([)]\"]", "false"}, {"[\"{[]}\"]", "true"}, {"[\"\"]", "true"}});
        }
    }

    private void configure(Problem problem, String name, String returnType, String[][] parameters, String starters) {
        FunctionSignature signature = new FunctionSignature(); signature.setFunctionName(name); signature.setReturnType(returnType);
        java.util.List<FunctionParameter> values = new java.util.ArrayList<>();
        for (int index = 0; index < parameters.length; index++) {
            FunctionParameter parameter = new FunctionParameter(); parameter.setName(parameters[index][0]); parameter.setType(parameters[index][1]); parameter.setParameterOrder(index); values.add(parameter);
        }
        signature.setParameters(values); problem.setFunctionSignature(signature); problem.setExecutionMode(ExecutionMode.FUNCTION); problem.setStarterCode(starters); problems.save(problem);
        for (TestCase test : tests.findByProblemId(problem.getId())) {
            if (test.getFunctionArguments() == null || test.getFunctionArguments().isBlank()) test.setFunctionArguments(arguments(test.getInputData()));
            test.setExpectedOutput(jsonValue(test.getExpectedOutput())); tests.save(test);
        }
    }

    private void addMissingHidden(Problem problem, String[][] candidates) {
        long present = tests.countByProblemIdAndHiddenTrue(problem.getId());
        for (String[] candidate : candidates) { if (present >= 4) break; add(problem, candidate[0], candidate[1], true); present++; }
    }

    private void add(Problem problem, String arguments, String expected, boolean hidden) {
        TestCase test = new TestCase(); test.setProblem(problem); test.setInputData(arguments); test.setFunctionArguments(arguments); test.setExpectedOutput(expected); test.setHidden(hidden); tests.save(test);
    }

    private String arguments(String raw) {
        try { JsonNode node = mapper.readTree(raw); if (node.isArray()) return node.toString(); ArrayNode array = mapper.createArrayNode(); array.add(node); return array.toString(); }
        catch (Exception ignored) { ArrayNode array = mapper.createArrayNode(); array.add(raw == null ? "" : raw.trim()); return array.toString(); }
    }

    private String jsonValue(String raw) {
        try { return mapper.readTree(raw).toString(); }
        catch (Exception ignored) { try { return mapper.writeValueAsString(raw == null ? "" : raw.trim()); } catch (Exception impossible) { return "\"\""; } }
    }
}
