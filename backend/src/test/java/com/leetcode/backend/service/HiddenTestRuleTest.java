package com.leetcode.backend.service;

import com.leetcode.backend.execution.FunctionValueCodec;
import com.leetcode.backend.model.*;
import com.leetcode.backend.repository.*;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HiddenTestRuleTest {
    @Test void onlyFourHiddenTestsAllowsPublishing() {
        for(long count=0;count<=4;count++) {
            ProblemRepository problems=mock(ProblemRepository.class); TestCaseRepository tests=mock(TestCaseRepository.class);
            Problem existing=problem(false), update=problem(true); when(problems.findById(1L)).thenReturn(Optional.of(existing)); when(tests.countByProblemIdAndHiddenTrue(1L)).thenReturn(count);
            ProblemService service=new ProblemService(problems,tests);
            if(count==4) assertDoesNotThrow(()->service.updateProblem(1L,update));
            else assertThrows(IllegalArgumentException.class,()->service.updateProblem(1L,update));
        }
    }

    @Test void fifthHiddenIsRejectedAndPublishedHiddenCannotBeRemovedOrMadePublic() {
        ProblemRepository problems=mock(ProblemRepository.class); TestCaseRepository tests=mock(TestCaseRepository.class);
        Problem published=problem(true); when(problems.findById(1L)).thenReturn(Optional.of(published)); when(tests.countByProblemIdAndHiddenTrue(1L)).thenReturn(4L);
        TestCaseService service=new TestCaseService(tests,problems,new FunctionValueCodec());
        TestCase fifth=valid(true); assertThrows(IllegalArgumentException.class,()->service.createTestCase(1L,fifth));
        TestCase existing=valid(true); existing.setProblem(published); when(tests.findById(9L)).thenReturn(Optional.of(existing));
        assertThrows(IllegalArgumentException.class,()->service.deleteTestCase(1L,9L));
        assertThrows(IllegalArgumentException.class,()->service.updateTestCase(1L,9L,valid(false)));
    }

    @Test void malformedFunctionMetadataCannotBePublished() {
        ProblemRepository problems=mock(ProblemRepository.class); TestCaseRepository tests=mock(TestCaseRepository.class);
        Problem existing=problem(false), update=problem(true); update.setFunctionSignature(null);
        when(problems.findById(1L)).thenReturn(Optional.of(existing)); when(tests.countByProblemIdAndHiddenTrue(1L)).thenReturn(4L);
        var error=assertThrows(IllegalArgumentException.class,()->new ProblemService(problems,tests).updateProblem(1L,update));
        assertTrue(error.getMessage().contains("function name and return type"));
    }

    private Problem problem(boolean active){Problem p=new Problem();p.setTitle("p");p.setDescription("d");p.setDifficulty("EASY");p.setActive(active);p.setExecutionMode(ExecutionMode.FUNCTION);
        FunctionSignature s=new FunctionSignature();s.setFunctionName("f");s.setReturnType("String");FunctionParameter fp=new FunctionParameter();fp.setName("s");fp.setType("String");s.setParameters(java.util.List.of(fp));p.setFunctionSignature(s);org.springframework.test.util.ReflectionTestUtils.setField(p,"id",1L);return p;}
    private TestCase valid(boolean hidden){TestCase t=new TestCase();t.setInputData("");t.setFunctionArguments("[\"a\"]");t.setExpectedOutput("\"a\"");t.setHidden(hidden);return t;}
}
