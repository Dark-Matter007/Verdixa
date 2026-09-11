package com.leetcode.backend.controller;
import com.leetcode.backend.dto.*; import com.leetcode.backend.service.*; import jakarta.servlet.http.HttpServletRequest; import jakarta.validation.Valid; import org.springframework.http.*; import org.springframework.security.core.Authentication; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/assistant") public class AssistantController {
 private final AssistantService service; private final AssistantRateLimiter limiter; public AssistantController(AssistantService s,AssistantRateLimiter l){service=s;limiter=l;}
 @PostMapping("/public/chat") public AssistantChatResponse publicChat(@Valid @RequestBody AssistantChatRequest r,HttpServletRequest request){if(!limiter.allowed("public:"+request.getRemoteAddr(),10))throw new org.springframework.web.server.ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Please wait a moment before sending another assistant message.");return service.publicChat(r);}
 @PostMapping("/chat") public AssistantChatResponse chat(@Valid @RequestBody AssistantChatRequest r,Authentication a){if(!limiter.allowed("user:"+a.getName(),20))throw new org.springframework.web.server.ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Please wait a moment before sending another assistant message.");return service.userChat(a.getName(),r);}
}
