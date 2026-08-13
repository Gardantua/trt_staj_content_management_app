package com.trt.contentengagement.leaderboardservice.infrastructure.messaging;

import com.trt.contentengagement.leaderboardservice.application.XpChangedEventHandler;
import com.trt.contentengagement.leaderboardservice.domain.XpChangedEventV1;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class XpChangedRabbitListener {
    private final ObjectMapper objectMapper;
    private final XpChangedEventHandler eventHandler;

    public XpChangedRabbitListener(ObjectMapper objectMapper, XpChangedEventHandler eventHandler) {
        this.objectMapper = objectMapper;
        this.eventHandler = eventHandler;
    }

    @RabbitListener(queues = LeaderboardRabbitTopology.QUEUE)
    public void receive(String payload) {
        eventHandler.handle(objectMapper.readValue(payload, XpChangedEventV1.class));
    }
}
