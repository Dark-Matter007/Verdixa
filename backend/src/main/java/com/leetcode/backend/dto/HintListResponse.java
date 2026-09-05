package com.leetcode.backend.dto;
import java.util.List;
public record HintListResponse(long total, long unlocked, long attempts, int unlockAt, boolean available, List<HintResponse> hints) {}
