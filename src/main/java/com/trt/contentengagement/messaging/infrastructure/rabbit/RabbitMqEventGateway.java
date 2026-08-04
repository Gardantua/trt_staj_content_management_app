package com.trt.contentengagement.messaging.infrastructure.rabbit;

import com.trt.contentengagement.messaging.application.OutboxEvent;
import com.trt.contentengagement.messaging.application.RabbitEventGateway;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
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
    private final Tracer tracer;
    private final Propagator propagator;

    public RabbitMqEventGateway(
            RabbitTemplate rabbitTemplate,
            Tracer tracer,
            Propagator propagator
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.tracer = tracer;
        this.propagator = propagator;
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
                        injectCurrentTrace(properties);
                        return message;
                    }
            );
            return operations.waitForConfirms(5_000);
        });
        if (!Boolean.TRUE.equals(confirmed)) {
            throw new AmqpException("RabbitMQ did not confirm the outbox event.");
        }
    }

    private void injectCurrentTrace(MessageProperties properties) {
        Span currentSpan = tracer.currentSpan();
        if (currentSpan != null) {
            propagator.inject(
                    currentSpan.context(),
                    properties,
                    (messageProperties, key, traceValue) ->
                            messageProperties.setHeader(key, traceValue)
            );
        }
    }
}
