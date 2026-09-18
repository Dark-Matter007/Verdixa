package com.leetcode.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leetcode.backend.model.TranslationCache;
import com.leetcode.backend.repository.TranslationCacheRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TranslationServiceTest {
    private HttpServer server;
    private String endpoint;
    private String authorization = "";
    private String requestBody = "";
    private int status = 200;
    private String response = "{\"translations\":[{\"text\":\"Analyse\"}]}";
    private int translateCalls;

    @BeforeEach void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v2/translate", this::translate);
        server.createContext("/v2/languages", exchange -> reply(exchange, 200, "[{\"language\":\"DE\"},{\"language\":\"HI\"},{\"language\":\"TE\"},{\"language\":\"PT-BR\"},{\"language\":\"PT-PT\"},{\"language\":\"ZH-HANS\"}]"));
        server.start();
        endpoint = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterEach void stopServer() { server.stop(0); }

    @Test void disabledProviderFallsBackToEnglishWithoutWritingCache() {
        TranslationCacheRepository repo = misses();
        assertEquals(List.of("Analytics"), service(repo, false).translate("en", "te", List.of("Analytics")));
        verify(repo, never()).save(any());
        assertEquals(0, translateCalls);
    }

    @Test void successfulDeepLTranslationUsesAuthorizationAndOneBatch() {
        response = "{\"translations\":[{\"text\":\"Armaturenbrett\"},{\"text\":\"Aufgaben\"}]}";
        assertEquals(List.of("Armaturenbrett", "Aufgaben"), service(misses(), true).translate("en", "de", List.of("Dashboard", "Problems")));
        assertEquals("DeepL-Auth-Key test-secret", authorization);
        assertTrue(requestBody.contains("\"source_lang\":\"EN\""));
        assertTrue(requestBody.contains("\"target_lang\":\"DE\""));
        assertEquals(1, translateCalls);
    }

    @Test void duplicateStringsAreDeduplicatedBeforeTheProvider() {
        TranslationCacheRepository repo = misses();
        assertEquals(List.of("Analyse", "Analyse"), service(repo, true).translate("en", "de", List.of("Analytics", "Analytics")));
        assertEquals(1, translateCalls);
        assertEquals(1, requestBody.split("Analytics", -1).length - 1);
        verify(repo).save(any());
    }

    @Test void persistentCachePreventsProviderCall() {
        TranslationCacheRepository repo = mock(TranslationCacheRepository.class);
        TranslationCache cached = new TranslationCache(); cached.setTranslatedText("Analyse");
        when(repo.findBySourceLanguageAndTargetLanguageAndSourceHash(anyString(), anyString(), anyString())).thenReturn(Optional.of(cached));
        assertEquals(List.of("Analyse"), service(repo, true).translate("en", "de", List.of("Analytics")));
        assertEquals(0, translateCalls);
    }

    @Test void quotaAndServerErrorsSafelyFallBackAndDoNotCache() {
        TranslationCacheRepository repo = misses();
        for (int failure : List.of(429, 456, 500)) {
            status = failure;
            assertEquals(List.of("Analytics"), service(repo, true).translate("en", "de", List.of("Analytics")));
        }
        verify(repo, never()).save(any());
        assertFalse(requestBody.contains("test-secret"));
    }

    @Test void malformedResponseFallsBackToEnglish() {
        response = "{\"translations\":[]}";
        assertEquals(List.of("Analytics"), service(misses(), true).translate("en", "de", List.of("Analytics")));
    }

    @Test void supportedLanguageMappingRetainsBundledAndScriptCodes() {
        assertTrue(service(misses(), true).supportedLanguages().containsAll(List.of("de", "hi", "te", "zh-HANS")));
    }

    @Test void endpointIsConfigurableForFreeOrProDeployments() {
        assertEquals(List.of("Analyse"), service(misses(), true).translate("en", "de", List.of("Analytics")));
        assertTrue(endpoint.startsWith("http://"));
    }

    @Test void missingApiKeyFallsBackWithoutCallingDeepL() {
        TranslationService service = new TranslationService(misses(), true, "", endpoint, HttpClient.newHttpClient(), new ObjectMapper());
        assertEquals(List.of("Analytics"), service.translate("en", "de", List.of("Analytics")));
        assertEquals(0, translateCalls);
    }

    @Test void unsupportedVerdixaLanguageFallsBackWithoutCallingDeepL() {
        assertEquals(List.of("Analytics"), service(misses(), true).translate("en", "zz", List.of("Analytics")));
        assertEquals(0, translateCalls);
    }

    @Test void chineseScriptCodeUsesDeepLHansValue() {
        assertEquals(List.of("Analyse"), service(misses(), true).translate("en", "zh-HANS", List.of("Analytics")));
        assertTrue(requestBody.contains("\"target_lang\":\"ZH-HANS\""));
    }

    @Test void portugueseRegionalCodeUsesDeepLRegionalValue() {
        assertEquals(List.of("Analyse"), service(misses(), true).translate("en", "pt-BR", List.of("Analytics")));
        assertTrue(requestBody.contains("\"target_lang\":\"PT-BR\""));
    }

    @Test void unreachableConfiguredUrlFallsBackWithoutLeakingCredentials() {
        TranslationService service = new TranslationService(misses(), true, "test-secret", "http://127.0.0.1:1", HttpClient.newHttpClient(), new ObjectMapper());
        assertEquals(List.of("Analytics"), service.translate("en", "de", List.of("Analytics")));
    }

    @Test void deepLFreeStyleCustomBaseUrlIsUsed() {
        assertEquals(List.of("Analyse"), service(misses(), true).translate("en", "de", List.of("Analytics")));
        assertEquals(1, translateCalls);
    }

    @Test void deepLProStyleCustomBaseUrlUsesTheSameContract() {
        assertEquals(List.of("Analyse"), service(misses(), true).translate("en", "de", List.of("Analytics")));
        assertEquals("DeepL-Auth-Key test-secret", authorization);
    }

    @Test void providerLanguageMetadataIsCachedForTheServiceLifetime() {
        TranslationService service = service(misses(), true);
        assertTrue(service.supportedLanguages().contains("te"));
        assertTrue(service.supportedLanguages().contains("hi"));
    }

    private TranslationCacheRepository misses() {
        TranslationCacheRepository repo = mock(TranslationCacheRepository.class);
        when(repo.findBySourceLanguageAndTargetLanguageAndSourceHash(anyString(), anyString(), anyString())).thenReturn(Optional.empty());
        return repo;
    }

    private TranslationService service(TranslationCacheRepository repo, boolean enabled) {
        return new TranslationService(repo, enabled, "test-secret", endpoint, HttpClient.newHttpClient(), new ObjectMapper());
    }

    private void translate(HttpExchange exchange) throws IOException {
        translateCalls++;
        authorization = exchange.getRequestHeaders().getFirst("Authorization");
        requestBody = new String(exchange.getRequestBody().readAllBytes());
        reply(exchange, status, response);
    }

    private static void reply(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes();
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
