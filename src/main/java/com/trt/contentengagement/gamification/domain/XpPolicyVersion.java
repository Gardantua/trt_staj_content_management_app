package com.trt.contentengagement.gamification.domain;

public enum XpPolicyVersion {
    SCORE_MATCH_V1,
    FIRST_COMPLETION_SCORE_V2,
    ADMIN_ADJUSTMENT_V1;

    public int xpForCompletedQuiz(int finalScore) {
        if (this != SCORE_MATCH_V1 && this != FIRST_COMPLETION_SCORE_V2) {
            throw new GamificationRuleViolationException(
                    "XP_POLICY_NOT_APPLICABLE",
                    "The XP policy cannot calculate a quiz completion reward."
            );
        }
        if (finalScore < 0) {
            throw new GamificationRuleViolationException(
                    "XP_SCORE_INVALID", "A completed attempt score cannot be negative."
            );
        }
        return finalScore;
    }
}
