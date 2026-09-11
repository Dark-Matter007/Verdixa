package com.leetcode.backend.config;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.userinfo.*;
import org.springframework.security.oauth2.core.user.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.*;

/** Uses GitHub's e-mail API because the profile e-mail can be intentionally private. */
@Service
public class VerdixaOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    private final DefaultOAuth2UserService users = new DefaultOAuth2UserService();
    private final RestClient github = RestClient.create("https://api.github.com");

    @Override public OAuth2User loadUser(OAuth2UserRequest request) {
        OAuth2User user = users.loadUser(request);
        Map<String, Object> attributes = new LinkedHashMap<>(user.getAttributes());
        Map<String, Object> email = verifiedGithubEmail(request);
        if (email != null) { attributes.put("email", email.get("email")); attributes.put("verdixa_email_verified", true); }
        else attributes.put("verdixa_email_verified", false);
        return new DefaultOAuth2User(user.getAuthorities(), attributes, "id");
    }

    private Map<String, Object> verifiedGithubEmail(OAuth2UserRequest request) {
        List<Map<String, Object>> emails = github.get().uri("/user/emails")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + request.getAccessToken().getTokenValue())
                .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .retrieve().body(new ParameterizedTypeReference<>() {});
        if (emails == null) return null;
        return emails.stream().filter(this::verified).filter(e -> Boolean.TRUE.equals(e.get("primary"))).findFirst()
                .or(() -> emails.stream().filter(this::verified).findFirst()).orElse(null);
    }
    private boolean verified(Map<String, Object> email) { return Boolean.TRUE.equals(email.get("verified")) && email.get("email") instanceof String value && !value.isBlank(); }
}
