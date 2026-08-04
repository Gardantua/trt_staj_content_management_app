package com.trt.contentengagement.messaging.infrastructure.rabbit;

import java.nio.charset.StandardCharsets;

import com.trt.contentengagement.messaging.application.QuizCompletedXpEventHandler;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
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
    private final ObservationRegistry observationRegistry;
    private final Tracer tracer;
    private final Propagator propagator;

    public QuizCompletedXpListener(
            QuizCompletedXpEventHandler eventHandler,
            MeterRegistry meterRegistry,
            ObservationRegistry observationRegistry,
            Tracer tracer,
            Propagator propagator
    ) {
        this.eventHandler = eventHandler;
        this.failedCounter = meterRegistry.counter("messaging.consumer.failed");
        this.observationRegistry = observationRegistry;
        this.tracer = tracer;
        this.propagator = propagator;
    }

    @RabbitListener(queues = RabbitMessagingTopology.XP_QUEUE)
    public void consume(Message message) {
        Object traceHeader = message.getMessageProperties().getHeader("x-trace-id");
        if (traceHeader != null) {
            MDC.put(EVENT_TRACE_ID_MDC_KEY, traceHeader.toString());
        }
        Span consumerSpan = consumerSpan(message);
        try {
            try (Tracer.SpanInScope ignored = tracer.withSpan(consumerSpan)) {
                Observation.createNotStarted("messaging.quiz.completed.consume", observationRegistry)
                        .lowCardinalityKeyValue("messaging.system", "rabbitmq")
                        .lowCardinalityKeyValue(
                                "messaging.destination", RabbitMessagingTopology.XP_QUEUE
                        )
                        .highCardinalityKeyValue(
                                "event.trace_id",
                                traceHeader == null ? "unknown" : traceHeader.toString()
                        )
                        .observe(() -> eventHandler.handle(
                                new String(message.getBody(), StandardCharsets.UTF_8)
                        ));
            }
        } catch (RuntimeException exception) {
            consumerSpan.error(exception);
            failedCounter.increment();
            throw exception;
        } finally {
            consumerSpan.end();
            MDC.remove(EVENT_TRACE_ID_MDC_KEY);
        }
    }

    private Span consumerSpan(Message message) {
        Span.Builder spanBuilder = propagator.extract(
                message.getMessageProperties(),
                (properties, key) -> {
                    Object header = properties.getHeaders().get(key);
                    return header == null ? null : header.toString();
                }
        );
        return spanBuilder
                .name("messaging.rabbitmq.consume")
                .kind(Span.Kind.CONSUMER)
                .tag("messaging.system", "rabbitmq")
                .tag("messaging.destination", RabbitMessagingTopology.XP_QUEUE)
                .start();
    }
}
