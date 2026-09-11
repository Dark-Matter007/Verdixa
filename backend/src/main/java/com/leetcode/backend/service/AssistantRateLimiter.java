package com.leetcode.backend.service;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.concurrent.ConcurrentHashMap;
/** Small in-memory guard; deploy behind an edge limiter for multi-instance production. */
@Service public class AssistantRateLimiter {
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    public boolean allowed(String key, int limit) { Instant now=Instant.now(); return windows.compute(key,(k,current)->{
        if(current==null || current.started.plus(Duration.ofMinutes(1)).isBefore(now)) return new Window(now,1);
        return current.count>=limit ? current : new Window(current.started,current.count+1);
    }).count<=limit; }
    private record Window(Instant started,int count) {}
}
