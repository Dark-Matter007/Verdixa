package com.leetcode.backend.controller;

import com.leetcode.backend.dto.*;
import com.leetcode.backend.service.ProfileSecurityService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final ProfileSecurityService profiles;
    public ProfileController(ProfileSecurityService profiles) { this.profiles = profiles; }

    @GetMapping public ProfileResponse profile(Authentication auth) { return profiles.getProfile(auth.getName()); }

    @PostMapping("/username/change/request")
    public ProfileActionResponse requestUsername(@Valid @RequestBody UsernameChangeRequest request, Authentication auth) {
        return profiles.requestUsernameChange(auth.getName(), request);
    }
    @PostMapping("/username/change/resend")
    public ProfileActionResponse resendUsername(@Valid @RequestBody UsernameChangeRequest request, Authentication auth) {
        return profiles.requestUsernameChange(auth.getName(), request);
    }
    @PostMapping("/username/change/verify")
    public ProfileActionResponse verifyUsername(@Valid @RequestBody OtpCodeRequest request, Authentication auth) {
        return profiles.verifyUsernameChange(auth.getName(), request);
    }

    @PostMapping("/email/change/request")
    public ProfileActionResponse requestEmail(@Valid @RequestBody EmailChangeRequest request, Authentication auth) {
        return profiles.requestEmailChange(auth.getName(), request);
    }
    @PostMapping("/email/change/resend-current")
    public ProfileActionResponse resendCurrentEmail(@Valid @RequestBody EmailChangeRequest request, Authentication auth) {
        return profiles.requestEmailChange(auth.getName(), request);
    }
    @PostMapping("/email/change/verify-current")
    public ProfileActionResponse verifyCurrentEmail(@Valid @RequestBody OtpCodeRequest request, Authentication auth) {
        return profiles.verifyCurrentEmail(auth.getName(), request);
    }
    @PostMapping("/email/change/resend-new")
    public ProfileActionResponse resendNewEmail(Authentication auth) { return profiles.resendNewEmailCode(auth.getName()); }
    @PostMapping("/email/change/verify-new")
    public ProfileActionResponse verifyNewEmail(@Valid @RequestBody OtpCodeRequest request, Authentication auth) {
        return profiles.verifyNewEmail(auth.getName(), request);
    }

    @PostMapping("/password/change")
    public ProfileActionResponse changePassword(@Valid @RequestBody PasswordChangeRequest request, Authentication auth) {
        return profiles.changePassword(auth.getName(), request);
    }
    @PostMapping("/password/setup/request")
    public ProfileActionResponse requestPasswordSetup(Authentication auth) { return profiles.requestPasswordSetup(auth.getName()); }
    @PostMapping("/password/setup/resend")
    public ProfileActionResponse resendPasswordSetup(Authentication auth) { return profiles.requestPasswordSetup(auth.getName()); }
    @PostMapping("/password/setup/verify")
    public ProfileActionResponse verifyPasswordSetup(@Valid @RequestBody OtpCodeRequest request, Authentication auth) {
        return profiles.verifyPasswordSetup(auth.getName(), request);
    }
    @PostMapping("/password/setup")
    public ProfileActionResponse setupPassword(@Valid @RequestBody PasswordSetupRequest request, Authentication auth) {
        return profiles.setupPassword(auth.getName(), request);
    }
}
