package com.trt.contentengagement.messaging.infrastructure.rabbit;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.messaging.rabbit-enabled", havingValue = "true")
public class RabbitMessagingConfiguration {
    @Bean
    DirectExchange contentEventsExchange() {
        return new DirectExchange(RabbitMessagingTopology.EVENTS_EXCHANGE, true, false);
    }

    @Bean
    DirectExchange contentDeadLetterExchange() {
        return new DirectExchange(RabbitMessagingTopology.DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    Queue quizCompletedXpQueue() {
        return QueueBuilder.durable(RabbitMessagingTopology.XP_QUEUE)
                .deadLetterExchange(RabbitMessagingTopology.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(RabbitMessagingTopology.XP_DEAD_LETTER_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue quizCompletedXpDeadLetterQueue() {
        return QueueBuilder.durable(RabbitMessagingTopology.XP_DEAD_LETTER_QUEUE).build();
    }

    @Bean
    Binding quizCompletedXpBinding(Queue quizCompletedXpQueue, DirectExchange contentEventsExchange) {
        return BindingBuilder.bind(quizCompletedXpQueue)
                .to(contentEventsExchange)
                .with(RabbitMessagingTopology.QUIZ_COMPLETED_ROUTING_KEY);
    }

    @Bean
    Binding quizCompletedXpDeadLetterBinding(
            Queue quizCompletedXpDeadLetterQueue,
            DirectExchange contentDeadLetterExchange
    ) {
        return BindingBuilder.bind(quizCompletedXpDeadLetterQueue)
                .to(contentDeadLetterExchange)
                .with(RabbitMessagingTopology.XP_DEAD_LETTER_ROUTING_KEY);
    }
}
