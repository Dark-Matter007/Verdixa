package com.leetcode.backend.config;

import com.fasterxml.jackson.databind.*;
import com.leetcode.backend.model.*;
import com.leetcode.backend.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;

/** Reviewed content, matched by title AND executable signature. Unknown contracts are never guessed. */
@Configuration
public class EditorialCatalogBackfill {
    private final ProblemRepository problems; private final EditorialRepository editorials;
    private final TransactionTemplate transaction;
    private final Map<String,JsonNode> catalog=new HashMap<>();
    public EditorialCatalogBackfill(ProblemRepository problems,EditorialRepository editorials,PlatformTransactionManager tm) throws java.io.IOException {
        this.problems=problems;this.editorials=editorials;this.transaction=new TransactionTemplate(tm);
        try(var input=new ClassPathResource("editorials/catalog.json").getInputStream()){
            for(JsonNode entry:new ObjectMapper().readTree(input))catalog.put(key(entry.path("problemTitle").asText()),entry);
        }
    }
    @Bean @Order(120) CommandLineRunner editorialBackfill(){return args->backfill();}
    public int backfill(){
        int populated=0;
        for(Long id:problems.findAll().stream().map(Problem::getId).toList()) {
            Boolean updated=transaction.execute(status->populate(id));if(Boolean.TRUE.equals(updated))populated++;
        }
        org.slf4j.LoggerFactory.getLogger(getClass()).info("Editorial backfill populated {} reviewed editorials",populated);
        return populated;
    }
    private boolean populate(Long id){
        // Lock the stable parent before checking the unique child, also across app instances.
        Problem p=problems.lockById(id).orElseThrow();
        Editorial existing=editorials.findByProblemId(id).orElse(null);
        if(existing!=null&&!replaceable(existing))return false;
        JsonNode content=catalog.get(key(p.getTitle()));
        if(content==null||!matches(p,content)){
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("No reviewed editorial for problem {} ({}) with this execution contract",id,p.getTitle());return false;
        }
        Editorial e=existing==null?new Editorial():existing;e.setProblem(p);
        e.setTitle(text(content,"title"));e.setIntuition(text(content,"intuition"));e.setKeyObservations(text(content,"keyObservations"));
        e.setApproach(text(content,"approach"));e.setAlgorithmExplanation(text(content,"algorithmExplanation"));e.setCorrectness(text(content,"correctness"));
        e.setEdgeCases(text(content,"edgeCases"));e.setTimeComplexity(text(content,"timeComplexity"));e.setSpaceComplexity(text(content,"spaceComplexity"));
        e.setReferenceGuidance(text(content,"referenceGuidance"));e.setJavaSolution(text(content,"javaSolution"));e.setCppSolution(text(content,"cppSolution"));e.setPythonSolution(text(content,"pythonSolution"));
        e.setPublished(true);e.setContentSource("VERDIXA_CATALOG_V1");editorials.saveAndFlush(e);return true;
    }
    private boolean replaceable(Editorial e){
        if("MANUAL".equals(e.getContentSource()))return false;
        // Do not republish an admin-unpublished default or rewrite the current version.
        if("VERDIXA_CATALOG_V1".equals(e.getContentSource()))return false;
        if("AUTO_GENERATED".equals(e.getContentSource())||"DEFAULT".equals(e.getContentSource()))return true;
        return java.util.stream.Stream.of(e.getIntuition(),e.getApproach(),e.getAlgorithmExplanation(),e.getCorrectness(),e.getKeyObservations(),e.getReferenceGuidance(),e.getEdgeCases(),e.getJavaSolution(),e.getCppSolution(),e.getPythonSolution()).allMatch(this::placeholder);
    }
    private boolean placeholder(String value){return value==null||value.isBlank()||Set.of("todo","tbd","coming soon","placeholder").contains(value.trim().toLowerCase(Locale.ROOT));}
    private boolean matches(Problem p,JsonNode c){
        FunctionSignature f=p.getFunctionSignature();
        if(p.getExecutionMode()!=ExecutionMode.FUNCTION||f==null||!f.getFunctionName().equals(text(c,"functionName"))||!f.getReturnType().equals(text(c,"returnType")))return false;
        var types=f.getParameters().stream().sorted(Comparator.comparingInt(FunctionParameter::getParameterOrder)).map(FunctionParameter::getType).toList();
        List<String> expected=new ArrayList<>();c.path("parameterTypes").forEach(t->expected.add(t.asText()));return types.equals(expected);
    }
    private static String text(JsonNode c,String field){return c.path(field).asText();}
    private static String key(String value){return value.trim().toLowerCase(Locale.ROOT);}
}
