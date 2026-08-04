package com.trt.contentengagement.messaging.application;

public interface RabbitEventGateway {
    void publish(OutboxEvent event);
}
