package com.leetcode.backend.dto;

import com.leetcode.backend.model.*;
import java.util.List;

/** Deliberately excludes password hashes, OTP state, auth version, provider subjects, and tokens. */
public record ProfileResponse(String username, String email, Role role, ThemePreference theme,
                              boolean emailVerified, boolean hasLocalPassword,
                              List<OAuthProvider> connectedProviders) { }
