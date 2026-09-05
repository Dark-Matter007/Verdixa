package com.leetcode.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leetcode.backend.execution.FunctionValueCodec;
import com.leetcode.backend.model.FunctionParameter;
import com.leetcode.backend.model.FunctionSignature;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Fast contract test for the data inserted by the idempotent catalog seeder. */
class VerdixaProblemCatalogSeederTest {
    private final FunctionValueCodec codec = new FunctionValueCodec();
    private final ObjectMapper json = new ObjectMapper();

    @Test void catalogHasFiftyCompleteExecutableProblemsIncludingTheThirtyProblemExpansion() throws Exception {
        List<VerdixaProblemCatalogSeeder.Spec> catalog = VerdixaProblemCatalogSeeder.catalog();
        assertEquals(50, catalog.size());
        assertEquals(Map.of("EASY", 10L, "MEDIUM", 24L, "HARD", 16L),
                catalog.stream().collect(java.util.stream.Collectors.groupingBy(VerdixaProblemCatalogSeeder.Spec::difficulty, java.util.stream.Collectors.counting())));
        assertEquals(50, catalog.stream().map(VerdixaProblemCatalogSeeder.Spec::title).map(String::toLowerCase).distinct().count());

        Set<String> expansionTitles = Set.of("Majority Signal", "Prefix Balance Point", "Single Number Trace", "First Unique Symbol",
                "Rotated Array Locator", "Peak Terrain Index", "Minimum Capacity Scheduler", "Kth Largest Stream Value", "Merge Time Blocks",
                "Minimum Meeting Rooms", "Next Greater Circular Value", "Sliding Window Maximum", "Subarray Target Count", "Longest Consecutive Chain",
                "Coin Combination Minimum", "Increasing Sequence Length", "Partition Target Feasibility", "Decode Message Ways", "Matrix Spiral Reader",
                "Connected Region Counter", "Median of Two Sorted Sequences", "Maximum Rectangle in Binary Grid", "Longest Valid Bracket Segment",
                "Burst Value Optimizer", "Palindrome Minimum Cuts", "Network Delay Minimum", "Cheapest Route With Stop Limit", "Maximum Job Profit Schedule",
                "Regex Pattern Matcher", "Course Completion Ordering");
        List<VerdixaProblemCatalogSeeder.Spec> expansion = catalog.stream().filter(s -> expansionTitles.contains(s.title())).toList();
        assertEquals(30, expansion.size());
        assertEquals(Map.of("EASY", 4L, "MEDIUM", 16L, "HARD", 10L),
                expansion.stream().collect(java.util.stream.Collectors.groupingBy(VerdixaProblemCatalogSeeder.Spec::difficulty, java.util.stream.Collectors.counting())));

        for (VerdixaProblemCatalogSeeder.Spec spec : catalog) {
            assertFalse(spec.description().isBlank()); assertFalse(spec.tags().isBlank());
            assertFalse(spec.constraints().isBlank()); assertFalse(spec.input().isBlank()); assertFalse(spec.output().isBlank());
            assertTrue(spec.examples().contains("->")); assertEquals(3, spec.hints().length);
            assertEquals(2, spec.cases().stream().filter(c -> !c.hidden()).count());
            assertEquals(4, spec.cases().stream().filter(VerdixaProblemCatalogSeeder.Case::hidden).count());
            FunctionSignature signature = VerdixaProblemCatalogSeeder.signature(spec); codec.validateSignature(signature);
            for (int i=0;i<signature.getParameters().size();i++) assertEquals(i, signature.getParameters().get(i).getParameterOrder());
            for (VerdixaProblemCatalogSeeder.Case test : spec.cases()) {
                codec.parseArguments(test.arguments(), signature.getParameters());
                codec.parse(test.expected(), signature.getReturnType());
            }
            Map<?,?> starters = json.readValue(VerdixaProblemCatalogSeeder.starters(spec), Map.class);
            assertTrue(starters.containsKey("java") && starters.containsKey("cpp") && starters.containsKey("python"));
            assertTrue(starters.values().stream().allMatch(value -> value instanceof String text && !text.isBlank()));
        }
    }
}
