package com.trt.contentengagement.messaging.infrastructure.rabbit;

public final class RabbitMessagingTopology {
    public static final String EVENTS_EXCHANGE = "content.engagement.events.v1";
    public static final String QUIZ_COMPLETED_ROUTING_KEY = "quiz.completed.v1";
    public static final String XP_CHANGED_ROUTING_KEY = "xp.changed.v1";
    public static final String XP_QUEUE = "gamification.quiz-completed-xp.v1";
    public static final String DEAD_LETTER_EXCHANGE = "content.engagement.dlx.v1";
    public static final String XP_DEAD_LETTER_ROUTING_KEY = "quiz.completed.xp.dead.v1";
    public static final String XP_DEAD_LETTER_QUEUE = "gamification.quiz-completed-xp.dead.v1";

    private RabbitMessagingTopology() {
    }
}
