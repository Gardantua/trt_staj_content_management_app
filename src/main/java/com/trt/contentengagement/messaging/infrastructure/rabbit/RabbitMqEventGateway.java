package com.trt.contentengagement.messaging.infrastructure.rabbit;

import com.trt.contentengagement.messaging.application.OutboxEvent;
import com.trt.contentengagement.messaging.application.RabbitEventGateway;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.messaging.rabbit-enabled", havingValue = "true")
public class RabbitMqEventGateway implements RabbitEventGateway {
    private final RabbitTemplate rabbitTemplate;

    public RabbitMqEventGateway(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitTemplate.setMandatory(true);
    }

    @Override
    public void publish(OutboxEvent event) {
        Boolean confirmed = rabbitTemplate.invoke(operations -> {
            operations.convertAndSend(
                    RabbitMessagingTopology.EVENTS_EXCHANGE,
                    RabbitMessagingTopology.QUIZ_COMPLETED_ROUTING_KEY,
                    event.payload(),
                    message -> {
                        MessageProperties properties = message.getMessageProperties();
                        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
                        properties.setContentEncoding("UTF-8");
                        properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                        properties.setMessageId(event.eventId().toString());
                        properties.setCorrelationId(event.traceId());
                        properties.setHeader("event-id", event.eventId().toString());
                        properties.setHeader("event-type", event.eventType());
                        properties.setHeader("event-version", event.eventVersion());
                        properties.setHeader("x-trace-id", event.traceId());
                        return message;
                    }
            );
            return operations.waitForConfirms(5_000);
        });
        if (!Boolean.TRUE.equals(confirmed)) {
            throw new AmqpException("RabbitMQ did not confirm the outbox event.");
        }
    }
}
