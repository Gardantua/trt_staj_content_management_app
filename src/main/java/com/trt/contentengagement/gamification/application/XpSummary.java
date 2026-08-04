package com.trt.contentengagement.gamification.application;

import java.util.UUID;

public record XpSummary(UUID userId, long totalXp, long transactionCount) {
}
