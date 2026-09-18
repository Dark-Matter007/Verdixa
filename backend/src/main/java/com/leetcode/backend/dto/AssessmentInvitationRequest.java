package com.leetcode.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Allows an owner to invite existing users or an external email address. */
public record AssessmentInvitationRequest(
        @Size(max = 100) List<Long> userIds,
        @Size(max = 100) List<@Email String> emails) { }
