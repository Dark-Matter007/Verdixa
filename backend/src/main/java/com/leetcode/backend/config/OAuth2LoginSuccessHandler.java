package com.leetcode.backend.config;

import com.leetcode.backend.model.OAuthProvider;
import com.leetcode.backend.model.User;
import com.leetcode.backend.service.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import java.io.IOException;
import java.util.Map;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {
    private final OAuthAccountService accounts; private final OAuthLoginCodeService codes; private final String successUrl; private final String failureUrl;
    public OAuth2LoginSuccessHandler(OAuthAccountService accounts, OAuthLoginCodeService codes, @Value("${verdixa.oauth.frontend-success-url}") String successUrl, @Value("${verdixa.oauth.frontend-failure-url}") String failureUrl) { this.accounts=accounts; this.codes=codes; this.successUrl=successUrl; this.failureUrl=failureUrl; }
    @Override public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        try {
            String registration = ((org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
            OAuth2User principal = (OAuth2User) authentication.getPrincipal(); Map<String,Object> a = principal.getAttributes();
            OAuthProvider provider = "google".equals(registration) ? OAuthProvider.GOOGLE : OAuthProvider.GITHUB;
            String id = string(a.get(provider == OAuthProvider.GOOGLE ? "sub" : "id"));
            boolean verified = provider == OAuthProvider.GOOGLE ? Boolean.TRUE.equals(a.get("email_verified")) : Boolean.TRUE.equals(a.get("verdixa_email_verified"));
            User user = accounts.resolve(new OAuthIdentity(provider, id, string(a.get("email")), verified, string(a.get("name")), string(a.get("login"))));
            response.sendRedirect(UriComponentsBuilder.fromUriString(successUrl).queryParam("code", codes.create(user)).build().encode().toUriString());
        } catch (OAuthLoginException exception) { redirectFailure(response, exception.getReason()); }
        catch (Exception exception) { redirectFailure(response, "oauth_failed"); }
    }
    private void redirectFailure(HttpServletResponse response, String reason) throws IOException { response.sendRedirect(UriComponentsBuilder.fromUriString(failureUrl).queryParam("reason", reason).build().encode().toUriString()); }
    private String string(Object value) { return value instanceof String string ? string : value == null ? null : String.valueOf(value); }
}
