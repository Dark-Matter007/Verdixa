package com.leetcode.backend.dto; import jakarta.validation.constraints.*; public record ContestProblemRequest(@NotNull Long problemId,@Min(1) Integer points){}
