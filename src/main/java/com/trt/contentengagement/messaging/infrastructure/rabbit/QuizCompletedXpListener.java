package com.trt.contentengagement.messaging.infrastructure.rabbit;

import java.nio.charset.StandardCharsets;

import com.trt.contentengagement.messaging.application.QuizCompletedXpEventHandler;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.messaging.rabbit-enabled", havingValue = "true")
public class QuizCompletedXpListener {
    private static final String EVENT_TRACE_ID_MDC_KEY = "eventTraceId";

    private final QuizCompletedXpEventHandler eventHandler;
    private final Counter failedCounter;

    public QuizCompletedXpListener(
            QuizCompletedXpEventHandler eventHandler,
            MeterRegistry meterRegistry
    ) {
        this.eventHandler = eventHandler;
        this.failedCounter = meterRegistry.counter("messaging.consumer.failed");
    }

    @RabbitListener(queues = RabbitMessagingTopology.XP_QUEUE)
    public void consume(Message message) {
        Object traceHeader = message.getMessageProperties().getHeader("x-trace-id");
        if (traceHeader != null) {
            MDC.put(EVENT_TRACE_ID_MDC_KEY, traceHeader.toString());
        }
        try {
            eventHandler.handle(new String(message.getBody(), StandardCharsets.UTF_8));
        } catch (RuntimeException exception) {
            failedCounter.increment();
            throw exception;
        } finally {
            MDC.remove(EVENT_TRACE_ID_MDC_KEY);
        }
    }
}
