package com.leetcode.backend.config;

import com.leetcode.backend.security.JwtAuthenticationFilter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import com.leetcode.backend.repository.UserRepository;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.http.HttpStatus;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final VerdixaOAuth2UserService oauth2UserService;
    private final OAuth2LoginSuccessHandler oauth2SuccessHandler;
    private final OAuth2LoginFailureHandler oauth2FailureHandler;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            VerdixaOAuth2UserService oauth2UserService,
            OAuth2LoginSuccessHandler oauth2SuccessHandler,
            OAuth2LoginFailureHandler oauth2FailureHandler) {

        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.oauth2UserService = oauth2UserService;
        this.oauth2SuccessHandler = oauth2SuccessHandler;
        this.oauth2FailureHandler = oauth2FailureHandler;
    }

    // ==========================
    // PASSWORD ENCODER
    // ==========================

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Prevents Spring's generated in-memory user and supports the application's user store. */
    @Bean
    public UserDetailsService userDetailsService(UserRepository users) {
        return username -> users.findByUsername(username)
                .map(user -> org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
                        .password(user.getPassword() == null ? "{noop}oauth-only-disabled" : user.getPassword()).authorities("ROLE_" + user.getRole().name()).build())
                .orElseThrow(() -> new UsernameNotFoundException("User not found."));
    }

    // ==========================
    // CORS CONFIGURATION
    // ==========================

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();

        configuration.setAllowedOrigins(
                List.of("http://localhost:5173")
        );

        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "DELETE",
                        "OPTIONS"
                )
        );

        configuration.setAllowedHeaders(
                List.of(
                        "Authorization",
                        "Content-Type"
                )
        );

        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }

    // ==========================
    // SECURITY FILTER CHAIN
    // ==========================

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http

                // API bearer-token calls do not use cookies. OAuth's session-backed state remains protected.
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))

                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())

                // Enable CORS
                .cors(cors -> cors.configurationSource(
                        corsConfigurationSource()
                ))

                // JWT APIs remain stateless; OAuth authorization state requires a short-lived HTTP session.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.IF_REQUIRED
                        )
                )

                // ==========================
                // AUTHORIZATION RULES
                // ==========================

                .authorizeHttpRequests(auth -> auth

                        // ==========================
                        // CORS PREFLIGHT
                        // ==========================

                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        ).permitAll()

                        // ==========================
                        // HEALTH
                        // ==========================

                        .requestMatchers(HttpMethod.GET, "/api/certificates/*", "/api/certificates/*/pdf").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/problems/*/editorial/reveal").hasAnyRole("USER", "ADMIN")

                        .requestMatchers(
                                "/api/health"
                        ).permitAll()

                        // ==========================
                        // AUTHENTICATION
                        // LOGIN + REGISTER
                        // ==========================

                        .requestMatchers(
                                "/api/auth/**"
                        ).permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/assistant/public/chat").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/assistant/chat").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/admin/assistant/**").hasRole("ADMIN")

                        .requestMatchers(
                                "/api/bookmarks/**"
                        ).hasRole("USER")

                        // ==========================
                        // CURRENT USER PROFILE + PROGRESS
                        // ==========================

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/users/me",
                                "/api/users/me/progress",
                                "/api/users/me/certificate-progress",
                                "/api/users/leaderboard",
                                "/api/users/leaderboard/page"
                        ).hasAnyRole("USER", "ADMIN")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/users/me/theme"
                        ).hasAnyRole("USER", "ADMIN")

                        // ==========================
                        // ADMIN USER MANAGEMENT
                        // ==========================

                        .requestMatchers(
                                "/api/users/**"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                "/api/admin/**"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/daily-challenges/admin"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/daily-challenges/**"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/daily-challenges/**"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/daily-challenges/**"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/contests",
                                "/api/contests/*/problems"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/contests/**"
                        ).hasRole("ADMIN")

                        // ==========================
                        // TEST CASE MANAGEMENT
                        // ADMIN ONLY
                        // ==========================

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/problems/*/testcases"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/problems/*/testcases/**"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/problems/*/testcases/**"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/problems/*/testcases/all"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                "/api/problems/*/hints/admin"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/problems/*/hints/*/reveal"
                        ).hasRole("USER")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/problems/admin/**"
                        ).hasRole("ADMIN")

                        // ==========================
                        // PUBLIC TEST CASES
                        // USER + ADMIN
                        // ==========================

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/problems/*/testcases"
                        ).hasAnyRole("USER", "ADMIN")

                        // ==========================
                        // PROBLEM MANAGEMENT
                        // ==========================

                        // USER + ADMIN can view problems
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/problems/**"
                        ).hasAnyRole("USER", "ADMIN")

                        // ADMIN can create problems
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/problems/**"
                        ).hasRole("ADMIN")

                        // ADMIN can update problems
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/problems/**"
                        ).hasRole("ADMIN")

                        // ADMIN can delete problems
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/problems/**"
                        ).hasRole("ADMIN")

                        // ==========================
                        // CODE EXECUTION
                        // USER + ADMIN
                        // ==========================

                        .requestMatchers(
                                "/api/execution-test/**"
                        ).hasAnyRole("USER", "ADMIN")

                        // ==========================
                        // EVERYTHING ELSE
                        // ==========================

                        .anyRequest().authenticated()
                )

                // ==========================
                // JWT FILTER
                // ==========================

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(userInfo -> userInfo.userService(oauth2UserService)
                                .oidcUserService(new org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService()))
                        .successHandler(oauth2SuccessHandler)
                        .failureHandler(oauth2FailureHandler))
                // OAuth endpoints above are explicitly public; protected Verdixa APIs retain their established 403 response.
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.FORBIDDEN)));

        return http.build();
    }
}
