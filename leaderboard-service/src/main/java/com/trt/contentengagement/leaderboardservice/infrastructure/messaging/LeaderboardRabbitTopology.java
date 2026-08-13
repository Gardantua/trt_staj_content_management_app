package com.trt.contentengagement.leaderboardservice.infrastructure.messaging;

public final class LeaderboardRabbitTopology {
    public static final String EVENTS_EXCHANGE = "content.engagement.events.v1";
    public static final String XP_CHANGED_ROUTING_KEY = "xp.changed.v1";
    public static final String QUEUE = "leaderboard.xp-changed.v1";
    public static final String DEAD_LETTER_EXCHANGE = "content.engagement.dlx.v1";
    public static final String DEAD_LETTER_ROUTING_KEY = "xp.changed.leaderboard.dead.v1";
    public static final String DEAD_LETTER_QUEUE = "leaderboard.xp-changed.dead.v1";

    private LeaderboardRabbitTopology() {
    }
}
