package com.leetcode.backend.execution;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leetcode.backend.model.FunctionParameter;
import com.leetcode.backend.model.FunctionSignature;
import org.springframework.stereotype.Component;
import java.util.*;

/** Central JSON contract for FUNCTION testcase values, literals, and comparison. */
@Component
public class FunctionValueCodec {
    private static final Set<String> TYPES = Set.of("int", "Integer", "long", "double", "boolean", "String", "int[]", "long[]", "double[]", "String[]");
    private static final double TOLERANCE = 1e-9;
    private final ObjectMapper mapper = new ObjectMapper();

    public void validateSignature(FunctionSignature signature) {
        if (signature == null) throw new IllegalArgumentException("Function signature is required.");
        if (!identifier(signature.getFunctionName())) throw new IllegalArgumentException("Function name is invalid.");
        requireType(signature.getReturnType());
        if (signature.getParameters() == null) throw new IllegalArgumentException("Function parameters are required.");
        for (FunctionParameter parameter : signature.getParameters()) {
            if (parameter == null || !identifier(parameter.getName())) throw new IllegalArgumentException("Function parameter name is invalid.");
            requireType(parameter.getType());
        }
    }

    public JsonNode parse(String json, String type) {
        requireType(type);
        try {
            JsonNode value = mapper.readTree(json);
            if (!matches(value, type)) throw new IllegalArgumentException("Value does not match " + type + ".");
            return value;
        } catch (JsonProcessingException exception) { throw new IllegalArgumentException("Invalid " + type + " JSON value."); }
    }

    public List<JsonNode> parseArguments(String json, List<FunctionParameter> parameters) {
        JsonNode values;
        try { values = json == null ? null : mapper.readTree(json); }
        catch (JsonProcessingException ex) { throw new IllegalArgumentException("Function arguments must be a JSON array."); }
        if (values == null || !values.isArray()) throw new IllegalArgumentException("Function arguments must be a JSON array.");
        if (values.size() != parameters.size()) throw new IllegalArgumentException("Argument count does not match the function signature.");
        List<JsonNode> result = new ArrayList<>();
        for (int i = 0; i < parameters.size(); i++) {
            String type = parameters.get(i).getType(); requireType(type);
            if (!matches(values.get(i), type)) throw new IllegalArgumentException("Argument " + (i + 1) + " does not match " + type + ".");
            result.add(values.get(i));
        }
        return result;
    }

    public String normalize(String value, String type) {
        try { return mapper.writeValueAsString(parse(value == null ? "" : value.trim(), type)); }
        catch (JsonProcessingException ex) { throw new IllegalStateException(ex); }
    }

    public boolean equivalent(String actual, String expected, String type) {
        JsonNode left = parse(actual == null ? "" : actual.trim(), type), right = parse(expected == null ? "" : expected.trim(), type);
        if ("double".equals(type)) return close(left.asDouble(), right.asDouble());
        if ("double[]".equals(type)) {
            if (left.size() != right.size()) return false;
            for (int i = 0; i < left.size(); i++) if (!close(left.get(i).asDouble(), right.get(i).asDouble())) return false;
            return true;
        }
        return left.equals(right);
    }

    public String javaLiteral(JsonNode value, String type) { return literal(value, type, "java"); }
    public String cppLiteral(JsonNode value, String type) { return literal(value, type, "cpp"); }
    public String pythonLiteral(JsonNode value, String type) { return literal(value, type, "python"); }

    private String literal(JsonNode value, String type, String language) {
        return switch (type) {
            case "int", "Integer" -> Integer.toString(value.intValue());
            case "long" -> value.longValue() + (language.equals("java") ? "L" : language.equals("cpp") ? "LL" : "");
            case "double" -> Double.toString(value.doubleValue());
            case "boolean" -> language.equals("python") ? (value.booleanValue() ? "True" : "False") : Boolean.toString(value.booleanValue());
            case "String" -> quote(value.textValue(), language.equals("python") ? '\'' : '"');
            case "int[]", "long[]", "double[]", "String[]" -> {
                String elementType = type.substring(0, type.length() - 2);
                String joined = join(value, elementType, language);
                yield language.equals("java") ? "new " + type + "{" + joined + "}" : language.equals("python") ? "[" + joined + "]" : "{" + joined + "}";
            }
            default -> throw unsupported(type);
        };
    }

    private String join(JsonNode array, String type, String language) {
        List<String> values = new ArrayList<>();
        array.forEach(node -> values.add(literal(node, type, language)));
        return String.join(", ", values);
    }

    private boolean matches(JsonNode value, String type) {
        if (value == null || value.isNull()) return false;
        return switch (type) {
            case "int", "Integer" -> value.isIntegralNumber() && value.canConvertToInt();
            case "long" -> value.isIntegralNumber() && value.canConvertToLong();
            case "double" -> value.isNumber() && Double.isFinite(value.doubleValue());
            case "boolean" -> value.isBoolean(); case "String" -> value.isTextual();
            case "int[]" -> arrayMatches(value, "int");
            case "long[]" -> arrayMatches(value, "long");
            case "double[]" -> arrayMatches(value, "double");
            case "String[]" -> arrayMatches(value, "String");
            default -> false;
        };
    }

    private boolean arrayMatches(JsonNode value, String elementType) {
        if (!value.isArray()) return false;
        for (JsonNode element : value) if (!matches(element, elementType)) return false;
        return true;
    }

    private String quote(String text, char delimiter) {
        StringBuilder result = new StringBuilder().append(delimiter);
        for (char c : text.toCharArray()) result.append(switch (c) {
            case '\\' -> "\\\\"; case '\n' -> "\\n"; case '\r' -> "\\r"; case '\t' -> "\\t";
            case '"' -> delimiter == '"' ? "\\\"" : "\""; case '\'' -> delimiter == '\'' ? "\\'" : "'";
            default -> c < 32 || c > 126 ? String.format("\\u%04x", (int)c) : Character.toString(c);
        });
        return result.append(delimiter).toString();
    }

    private boolean close(double a, double b) { return Math.abs(a - b) <= TOLERANCE * Math.max(1.0, Math.max(Math.abs(a), Math.abs(b))); }
    private boolean identifier(String value) { return value != null && value.matches("[A-Za-z_][A-Za-z0-9_]*"); }
    private void requireType(String type) { if (!TYPES.contains(type)) throw unsupported(type); }
    private IllegalArgumentException unsupported(String type) { return new IllegalArgumentException("Unsupported FUNCTION type: " + type); }
}
