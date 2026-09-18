package com.leetcode.backend.dto;
import jakarta.validation.constraints.Pattern;
public record LanguagePreferenceRequest(@Pattern(regexp="^[a-z]{2,3}(-[A-Z]{2})?$",message="Invalid language code") String language){}
