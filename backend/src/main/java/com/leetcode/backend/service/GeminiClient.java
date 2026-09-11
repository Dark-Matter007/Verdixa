package com.leetcode.backend.service;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI; import java.net.http.*; import java.time.Duration; import java.util.*;
@Service public class GeminiClient {
    private final boolean enabled; private final String key; private final String model; private final ObjectMapper json = new ObjectMapper(); private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    public GeminiClient(@Value("${verdixa.gemini.enabled:false}") boolean enabled,@Value("${verdixa.gemini.api-key:}") String key,@Value("${verdixa.gemini.model:gemini-2.5-flash}") String model){this.enabled=enabled;this.key=key;this.model=model;}
    public Optional<String> respond(String instruction,String question,String context) {
        return generate(instruction, question, context, null);
    }
    private Optional<String> generate(String instruction,String question,String context,List<VerdixaAssistantScope> intentValues) {
        if(!enabled||key.isBlank()) return Optional.empty();
        try { ObjectNode body=json.createObjectNode(); ObjectNode system=body.putObject("system_instruction"); system.putArray("parts").addObject().put("text",instruction);
            ObjectNode content=body.putArray("contents").addObject(); content.putArray("parts").addObject().put("text", "VERDIXA_CONTEXT:\n"+context+"\nEND_CONTEXT\nUSER_MESSAGE:\n"+question);
            ObjectNode generation=body.putObject("generationConfig"); generation.put("temperature",intentValues==null?0.2:0).put("maxOutputTokens",intentValues==null?350:40);
            if(intentValues!=null){generation.put("responseMimeType","application/json");ObjectNode schema=generation.putObject("responseSchema");schema.put("type","OBJECT");ObjectNode intent=schema.putObject("properties").putObject("intent");intent.put("type","STRING");var intentEnum=intent.putArray("enum");for(VerdixaAssistantScope value:intentValues)intentEnum.add(value.name());schema.putArray("required").add("intent");}
            HttpRequest request=HttpRequest.newBuilder(URI.create("https://generativelanguage.googleapis.com/v1beta/models/"+model+":generateContent?key="+key)).timeout(Duration.ofSeconds(12)).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build();
            HttpResponse<String> response=http.send(request,HttpResponse.BodyHandlers.ofString()); if(response.statusCode()/100!=2) return Optional.empty();
            JsonNode text=json.readTree(response.body()).at("/candidates/0/content/parts/0/text"); return text.isTextual()&& !text.asText().isBlank()?Optional.of(text.asText().trim()):Optional.empty();
        } catch(Exception ignored) { return Optional.empty(); }
    }

    /** Semantic classification is advisory only. Callers still enforce every permission deterministically. */
    public Optional<VerdixaAssistantScope> classifyIntent(String question, String pageType, VerdixaAssistantAudience audience) {
        if (!enabled || key.isBlank()) return Optional.empty();
        List<VerdixaAssistantScope> values = List.of(VerdixaAssistantScope.PUBLIC_HELP, VerdixaAssistantScope.PUBLIC_BENEFITS, VerdixaAssistantScope.USER_DATA, VerdixaAssistantScope.USER_SUMMARY, VerdixaAssistantScope.USER_SUBMISSIONS, VerdixaAssistantScope.USER_CONTESTS, VerdixaAssistantScope.USER_CERTIFICATES, VerdixaAssistantScope.ADMIN_DATA, VerdixaAssistantScope.ADMIN_SUMMARY, VerdixaAssistantScope.VERDIXA_EXPLANATION, VerdixaAssistantScope.VERDIXA_NAVIGATION, VerdixaAssistantScope.UNKNOWN_VERDIXA_HELP, VerdixaAssistantScope.ADMIN_ACTION, VerdixaAssistantScope.OUT_OF_SCOPE);
        String allowed = values.stream().map(Enum::name).reduce((left,right)->left+", "+right).orElse("");
        String instruction = "Classify the message for the Verdixa coding platform. Return JSON only as {\"intent\":\"ONE_ENUM\"}. Allowed values: " + allowed + ". Do not answer the message and do not make authorization decisions.";
        String context = "audience=" + audience + "\npageType=" + (pageType == null ? "UNKNOWN" : pageType);
        return generate(instruction, question, context, values).flatMap(value -> {
            try {
                JsonNode root = json.readTree(value);
                if (!root.isObject() || root.size() != 1 || !root.path("intent").isTextual()) return Optional.empty();
                VerdixaAssistantScope intent = VerdixaAssistantScope.valueOf(root.path("intent").asText());
                return intent == VerdixaAssistantScope.SENSITIVE_REQUEST ? Optional.empty() : Optional.of(intent);
            } catch (Exception ignored) {
                return Optional.empty();
            }
        });
    }
}
