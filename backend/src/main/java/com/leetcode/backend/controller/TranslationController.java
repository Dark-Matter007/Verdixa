package com.leetcode.backend.controller;

import com.leetcode.backend.service.TranslationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/i18n")
public class TranslationController {
    private static final int MAX_BATCHES_PER_MINUTE = 60;
    private final TranslationService service;
    private final ConcurrentHashMap<String, RateWindow> rateWindows = new ConcurrentHashMap<>();

    TranslationController(TranslationService service) {
        this.service = service;
    }

    public record Request(
            @Pattern(regexp = "^[a-z]{2,3}(?:-[A-Za-z]{2,8})?$") String sourceLanguage,
            @Pattern(regexp = "^[a-z]{2,3}(?:-[A-Za-z]{2,8})?$") String targetLanguage,
            @NotEmpty @Size(max = 50) List<@Size(max = 4000) String> texts) {}

    @PostMapping("/translate-batch")
    public Map<String, Object> translate(@Valid @RequestBody Request request, HttpServletRequest servletRequest) {
        enforceRateLimit(clientAddress(servletRequest));
        return Map.of("translations", service.translate(
                request.sourceLanguage(), request.targetLanguage(), request.texts()));
    }

    @GetMapping("/languages")
    public Map<String, Object> languages() { return Map.of("languages", service.supportedLanguages()); }

    private void enforceRateLimit(String client) {
        long minute = Instant.now().getEpochSecond() / 60;
        RateWindow window = rateWindows.computeIfAbsent(client, ignored -> new RateWindow());
        synchronized (window) {
            if (window.minute != minute) {
                window.minute = minute;
                window.count = 0;
            }
            if (++window.count > MAX_BATCHES_PER_MINUTE) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Translation request limit exceeded.");
            }
        }
        if (rateWindows.size() > 10_000) {
            rateWindows.entrySet().removeIf(entry -> entry.getValue().minute < minute - 1);
        }
    }

    private String clientAddress(HttpServletRequest request) {
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }

    private static final class RateWindow {
        private long minute;
        private int count;
    }
}
