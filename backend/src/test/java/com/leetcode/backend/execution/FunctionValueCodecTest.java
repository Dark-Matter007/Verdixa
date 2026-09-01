package com.leetcode.backend.execution;

import com.leetcode.backend.model.FunctionParameter;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class FunctionValueCodecTest {
    private final FunctionValueCodec codec = new FunctionValueCodec();

    @Test void rejectsMalformedJsonWrongCountAndWrongType() {
        List<FunctionParameter> parameters = List.of(parameter("s", "String"));
        assertThrows(IllegalArgumentException.class, () -> codec.parseArguments("[", parameters));
        assertThrows(IllegalArgumentException.class, () -> codec.parseArguments("[]", parameters));
        assertThrows(IllegalArgumentException.class, () -> codec.parseArguments("[1]", parameters));
    }

    @Test void validatesRangesArraysAndExpectedReturns() {
        assertThrows(IllegalArgumentException.class, () -> codec.parse("2147483648", "int"));
        assertEquals("[1,2,3]", codec.normalize("[1, 2, 3]", "int[]"));
        assertEquals("[\"a\",\"b\"]", codec.normalize("[\"a\", \"b\"]", "String[]"));
        assertThrows(IllegalArgumentException.class, () -> codec.parse("[1,\"x\"]", "long[]"));
        assertThrows(IllegalArgumentException.class, () -> codec.parse("true", "String"));
    }

    @Test void comparesDoublesWithTolerance() {
        assertTrue(codec.equivalent("1.0000000005", "1.0", "double"));
        assertFalse(codec.equivalent("1.00001", "1.0", "double"));
        assertTrue(codec.equivalent("[1.0,2.0000000001]", "[1,2]", "double[]"));
    }

    @Test void emitsEscapedLanguageSafeLiterals() {
        var value = codec.parse("\"a\\n\\\"b\\\\c\"", "String");
        assertEquals("\"a\\n\\\"b\\\\c\"", codec.javaLiteral(value, "String"));
        assertEquals("\"a\\n\\\"b\\\\c\"", codec.cppLiteral(value, "String"));
        assertEquals("'a\\n\"b\\\\c'", codec.pythonLiteral(value, "String"));
    }

    static FunctionParameter parameter(String name, String type) {
        FunctionParameter p = new FunctionParameter(); p.setName(name); p.setType(type); return p;
    }
}
