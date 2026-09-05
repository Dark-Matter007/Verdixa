package com.leetcode.backend.dto;
import com.leetcode.backend.model.ProblemHint;

public record HintResponse(
        Long id,
        String title,
        String content,
        int displayOrder,
        int orderIndex,
        int penaltyPoints,
        boolean active,
        boolean revealed,
        boolean available,
        boolean locked,
        int attemptsRequired,
        long attemptsRemaining) {

    public static HintResponse admin(ProblemHint hint) {
        return new HintResponse(hint.getId(), hint.getTitle(), hint.getContent(), hint.getDisplayOrder(),
                hint.getDisplayOrder(), hint.getPenaltyPoints(), hint.isActive(), true, true, false, 0, 0);
    }

    public static HintResponse user(ProblemHint hint, int orderIndex, long attempts, boolean revealed) {
        int attemptsRequired = orderIndex * 3;
        boolean available = attempts >= attemptsRequired;
        return new HintResponse(hint.getId(), revealed ? hint.getTitle() : null, revealed ? hint.getContent() : null,
                hint.getDisplayOrder(), orderIndex, hint.getPenaltyPoints(), true, revealed, available,
                !available, attemptsRequired, Math.max(0, attemptsRequired - attempts));
    }
}
