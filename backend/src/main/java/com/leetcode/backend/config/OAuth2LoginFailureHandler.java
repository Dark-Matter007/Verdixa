package com.leetcode.backend.config;

import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import java.io.IOException;

@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {
    private final String failureUrl;
    public OAuth2LoginFailureHandler(@Value("${verdixa.oauth.frontend-failure-url}") String failureUrl) { this.failureUrl = failureUrl; }
    @Override public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException { response.sendRedirect(UriComponentsBuilder.fromUriString(failureUrl).queryParam("reason", "oauth_failed").build().encode().toUriString()); }
}
