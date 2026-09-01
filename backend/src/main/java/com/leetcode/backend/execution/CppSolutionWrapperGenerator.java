package com.leetcode.backend.execution;

import com.fasterxml.jackson.databind.JsonNode;
import com.leetcode.backend.model.FunctionSignature;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class CppSolutionWrapperGenerator implements SolutionWrapperGenerator {
    private final FunctionValueCodec codec;
    public CppSolutionWrapperGenerator(FunctionValueCodec codec) { this.codec = codec; }
    @Override public String language() { return "cpp"; }

    @Override public String generate(FunctionSignature signature, String userSource, List<JsonNode> arguments) {
        List<String> literals = new ArrayList<>();
        for (int i=0;i<arguments.size();i++) literals.add(codec.cppLiteral(arguments.get(i), signature.getParameters().get(i).getType()));
        return """
                #include <bits/stdc++.h>
                using namespace std;
                %s
                string jsonString(const string& s) { string r="\\\""; for(unsigned char c:s) { switch(c) { case '\\\\':r+="\\\\\\\\";break; case '\"':r+="\\\\\\\"";break; case '\\n':r+="\\\\n";break; case '\\r':r+="\\\\r";break; case '\\t':r+="\\\\t";break; default:r+=c; } } return r+"\\\""; }
                string toJson(const string& v) { return jsonString(v); }
                string toJson(bool v) { return v ? "true" : "false"; }
                template<class T> string toJson(T v) { ostringstream o; o << setprecision(17) << v; return o.str(); }
                template<class T> string toJson(const vector<T>& v) { string r="["; for(size_t i=0;i<v.size();++i) { if(i) r+=","; r+=toJson(v[i]); } return r+"]"; }
                int main() { auto result = %s(%s); cout << toJson(result); }
                """.formatted(userSource, signature.getFunctionName(), String.join(", ", literals));
    }
}
