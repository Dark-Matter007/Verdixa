package com.leetcode.backend.dto;

/** Returned after only contest-specific mappings and registrations have been removed. */
public record AdminContestDeleteResponse(Long id, String message) { }
