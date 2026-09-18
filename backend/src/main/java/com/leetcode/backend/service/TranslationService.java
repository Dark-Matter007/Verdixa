package com.leetcode.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leetcode.backend.model.TranslationCache;
import com.leetcode.backend.repository.TranslationCacheRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/** Server-only DeepL boundary: provider failures always return the English source. */
@Service
public class TranslationService {
    private static final Logger log = LoggerFactory.getLogger(TranslationService.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(8);
    private static final Duration LANGUAGE_CACHE_TTL = Duration.ofHours(6);
    /** Verdixa code -> exact DeepL target code. Regional/script codes are never truncated. */
    private static final Map<String, String> VERDIXA_TO_DEEPL = Map.ofEntries(
            Map.entry("en", "EN-US"), Map.entry("en-US", "EN-US"), Map.entry("en-GB", "EN-GB"),
            Map.entry("bg", "BG"), Map.entry("cs", "CS"), Map.entry("da", "DA"), Map.entry("de", "DE"),
            Map.entry("el", "EL"), Map.entry("es", "ES"), Map.entry("et", "ET"), Map.entry("fi", "FI"),
            Map.entry("fr", "FR"), Map.entry("hu", "HU"), Map.entry("id", "ID"), Map.entry("it", "IT"),
            Map.entry("ja", "JA"), Map.entry("ko", "KO"), Map.entry("lt", "LT"), Map.entry("lv", "LV"),
            Map.entry("nl", "NL"), Map.entry("no", "NB"), Map.entry("pl", "PL"), Map.entry("pt", "PT-PT"),
            Map.entry("pt-BR", "PT-BR"), Map.entry("pt-PT", "PT-PT"), Map.entry("ro", "RO"), Map.entry("ru", "RU"),
            Map.entry("sk", "SK"), Map.entry("sl", "SL"), Map.entry("sv", "SV"), Map.entry("tr", "TR"),
            Map.entry("uk", "UK"), Map.entry("zh-CN", "ZH-HANS"), Map.entry("zh-TW", "ZH-HANT"),
            Map.entry("zh-HANS", "ZH-HANS"), Map.entry("zh-HANT", "ZH-HANT"), Map.entry("ar", "AR"),
            Map.entry("hi", "HI"), Map.entry("te", "TE"), Map.entry("he", "HE"), Map.entry("vi", "VI"),
            Map.entry("th", "TH"), Map.entry("ms", "MS"), Map.entry("ca", "CA"), Map.entry("af", "AF"),
            Map.entry("sq", "SQ"), Map.entry("hy", "HY"), Map.entry("az", "AZ"), Map.entry("bn", "BN"),
            Map.entry("eu", "EU"), Map.entry("gl", "GL"), Map.entry("ka", "KA"), Map.entry("gu", "GU"),
            Map.entry("is", "IS"), Map.entry("kk", "KK"), Map.entry("km", "KM"), Map.entry("lo", "LO"),
            Map.entry("mk", "MK"), Map.entry("mr", "MR"), Map.entry("my", "MY"), Map.entry("ne", "NE"),
            Map.entry("pa", "PA"), Map.entry("sr", "SR"), Map.entry("sw", "SW"), Map.entry("ta", "TA"),
            Map.entry("ur", "UR")
    );
    private static final Set<String> FALLBACK_DEEPL_TARGETS = Set.copyOf(VERDIXA_TO_DEEPL.values());

    private final TranslationCacheRepository cache;
    private final ObjectMapper mapper;
    private final HttpClient httpClient;
    private final boolean enabled;
    private final String apiKey;
    private final String apiUrl;
    private final ConcurrentHashMap<String, CompletableFuture<String>> inFlight = new ConcurrentHashMap<>();
    private volatile Set<String> providerTargets = FALLBACK_DEEPL_TARGETS;
    private volatile Instant providerTargetsFetchedAt = Instant.EPOCH;

    @Autowired
    public TranslationService(TranslationCacheRepository cache,
            @Value("${verdixa.deepl.enabled:false}") boolean enabled,
            @Value("${verdixa.deepl.api-key:}") String apiKey,
            @Value("${verdixa.deepl.api-url:https://api-free.deepl.com}") String apiUrl) {
        this(cache, enabled, apiKey, apiUrl, HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build(), new ObjectMapper());
    }

    TranslationService(TranslationCacheRepository cache, boolean enabled, String apiKey, String apiUrl,
                       HttpClient httpClient, ObjectMapper mapper) {
        this.cache = cache;
        this.enabled = enabled;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.apiUrl = trimTrailingSlash(apiUrl);
        this.httpClient = httpClient;
        this.mapper = mapper;
    }

    @Transactional
    public List<String> translate(String source, String target, List<String> texts) {
        if (texts == null || texts.isEmpty()) return List.of();
        if (texts.size() > 50) throw new IllegalArgumentException("At most 50 strings may be translated at once.");
        if (texts.stream().anyMatch(text -> text == null || text.length() > 4000)) throw new IllegalArgumentException("Each translatable string must be at most 4000 characters.");
        String sourceCode = normalizeVerdixaCode(source);
        String targetCode = normalizeVerdixaCode(target);
        if (sourceCode.equals(targetCode) || targetCode.equals("en") || !VERDIXA_TO_DEEPL.containsKey(targetCode)) return List.copyOf(texts);

        List<String> results = new ArrayList<>(texts.size());
        Map<String, List<Integer>> missingByText = new LinkedHashMap<>();
        for (int index = 0; index < texts.size(); index++) {
            String text = texts.get(index);
            var cached = cache.findBySourceLanguageAndTargetLanguageAndSourceHash(sourceCode, targetCode, hash(text));
            if (cached.isPresent()) results.add(cached.get().getTranslatedText());
            else { results.add(null); missingByText.computeIfAbsent(text, ignored -> new ArrayList<>()).add(index); }
        }
        if (missingByText.isEmpty() || !providerConfigured() || !isProviderTargetSupported(targetCode)) {
            fillEnglishFallback(results, texts);
            return results;
        }
        try {
            Map<String, String> translated = translateUnique(sourceCode, targetCode, new ArrayList<>(missingByText.keySet()));
            for (Map.Entry<String, List<Integer>> entry : missingByText.entrySet()) {
                String value = translated.get(entry.getKey());
                if (value == null || value.isBlank()) continue;
                entry.getValue().forEach(index -> results.set(index, value));
                cacheSuccessfulTranslation(sourceCode, targetCode, entry.getKey(), value);
            }
        } catch (Exception ignored) {
            // requestDeepL has already logged a safe, credential-free summary.
        }
        fillEnglishFallback(results, texts);
        return results;
    }

    /** Returns stable Verdixa codes; DeepL metadata is cached and has a local safe fallback. */
    public List<String> supportedLanguages() {
        Set<String> targets = refreshProviderTargetsIfNeeded();
        return VERDIXA_TO_DEEPL.entrySet().stream().filter(entry -> targets.contains(entry.getValue()))
                .map(Map.Entry::getKey).distinct().sorted().toList();
    }

    private Map<String, String> translateUnique(String source, String target, List<String> texts) {
        Map<String, CompletableFuture<String>> futures = new LinkedHashMap<>();
        List<String> owned = new ArrayList<>();
        for (String text : texts) {
            String key = inFlightKey(source, target, text);
            CompletableFuture<String> created = new CompletableFuture<>();
            CompletableFuture<String> existing = inFlight.putIfAbsent(key, created);
            futures.put(text, existing == null ? created : existing);
            if (existing == null) owned.add(text);
        }
        if (!owned.isEmpty()) {
            try {
                List<String> translated = requestDeepL(source, target, owned);
                if (translated.size() != owned.size()) throw new IllegalStateException("incomplete response");
                for (int index = 0; index < owned.size(); index++) futures.get(owned.get(index)).complete(translated.get(index));
            } catch (Exception failure) {
                owned.forEach(text -> futures.get(text).completeExceptionally(failure));
            } finally {
                owned.forEach(text -> inFlight.remove(inFlightKey(source, target, text), futures.get(text)));
            }
        }
        Map<String, String> output = new LinkedHashMap<>();
        futures.forEach((text, future) -> output.put(text, future.join()));
        return output;
    }

    private List<String> requestDeepL(String source, String target, List<String> texts) throws Exception {
        Map<String, Object> body = Map.of("text", texts, "source_lang", deepLSource(source), "target_lang", VERDIXA_TO_DEEPL.get(target));
        HttpRequest request = HttpRequest.newBuilder(URI.create(apiUrl + "/v2/translate")).timeout(REQUEST_TIMEOUT)
                .header("Authorization", "DeepL-Auth-Key " + apiKey).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build();
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception exception) {
            log.warn("DeepL translation request failed. Falling back to English.");
            throw exception;
        }
        if (response.statusCode() / 100 != 2) { log.warn("DeepL translation unavailable (HTTP {}). Falling back to English.", response.statusCode()); throw new IllegalStateException("provider response"); }
        JsonNode values = mapper.readTree(response.body()).path("translations");
        if (!values.isArray() || values.size() != texts.size()) { log.warn("DeepL translation returned an invalid batch. Falling back to English."); throw new IllegalStateException("invalid response"); }
        List<String> translated = new ArrayList<>(texts.size());
        for (JsonNode value : values) {
            String text = value.path("text").asText("");
            if (text.isBlank()) { log.warn("DeepL translation returned an invalid value. Falling back to English."); throw new IllegalStateException("invalid response"); }
            translated.add(text);
        }
        return translated;
    }

    private boolean isProviderTargetSupported(String code) { return refreshProviderTargetsIfNeeded().contains(VERDIXA_TO_DEEPL.get(code)); }
    private Set<String> refreshProviderTargetsIfNeeded() {
        if (!providerConfigured() || providerTargetsFetchedAt.plus(LANGUAGE_CACHE_TTL).isAfter(Instant.now())) return providerTargets;
        synchronized (this) {
            if (providerTargetsFetchedAt.plus(LANGUAGE_CACHE_TTL).isAfter(Instant.now())) return providerTargets;
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(apiUrl + "/v2/languages?type=target")).timeout(REQUEST_TIMEOUT)
                        .header("Authorization", "DeepL-Auth-Key " + apiKey).GET().build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                JsonNode languages = response.statusCode() / 100 == 2 ? mapper.readTree(response.body()) : null;
                Set<String> targets = new LinkedHashSet<>();
                if (languages != null && languages.isArray()) languages.forEach(language -> {
                    String code = language.path("language").asText("").toUpperCase(Locale.ROOT);
                    if (!code.isBlank()) targets.add(code);
                });
                if (targets.isEmpty()) throw new IllegalStateException("invalid language metadata");
                providerTargets = Set.copyOf(targets);
            } catch (Exception ignored) {
                providerTargets = FALLBACK_DEEPL_TARGETS;
                log.warn("DeepL language metadata unavailable. Using the local language registry.");
            }
            providerTargetsFetchedAt = Instant.now();
            return providerTargets;
        }
    }

    private void cacheSuccessfulTranslation(String source, String target, String original, String translated) {
        TranslationCache item = new TranslationCache(); item.setSourceLanguage(source); item.setTargetLanguage(target);
        item.setSourceHash(hash(original)); item.setTranslatedText(translated); cache.save(item);
    }
    private static void fillEnglishFallback(List<String> results, List<String> texts) { for (int i = 0; i < results.size(); i++) if (results.get(i) == null) results.set(i, texts.get(i)); }
    private boolean providerConfigured() { return enabled && !apiKey.isBlank(); }
    private static String deepLSource(String source) { return source.equals("en") ? "EN" : source.toUpperCase(Locale.ROOT); }
    private static String normalizeVerdixaCode(String code) { return code == null ? "" : code.trim().replace('_', '-'); }
    private static String trimTrailingSlash(String url) { return (url == null || url.isBlank() ? "https://api-free.deepl.com" : url.trim()).replaceAll("/+$", ""); }
    private static String inFlightKey(String source, String target, String text) { return source + '\u0000' + target + '\u0000' + hash(text); }
    private static String hash(String text) { try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))); } catch (Exception exception) { throw new IllegalStateException("Unable to hash translation source.", exception); } }
}
