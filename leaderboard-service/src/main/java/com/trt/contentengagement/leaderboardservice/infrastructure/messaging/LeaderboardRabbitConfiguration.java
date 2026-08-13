package com.trt.contentengagement.leaderboardservice.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LeaderboardRabbitConfiguration {
    @Bean
    DirectExchange contentEventsExchange() {
        return new DirectExchange(LeaderboardRabbitTopology.EVENTS_EXCHANGE, true, false);
    }

    @Bean
    DirectExchange contentDeadLetterExchange() {
        return new DirectExchange(LeaderboardRabbitTopology.DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    Queue leaderboardXpQueue() {
        return QueueBuilder.durable(LeaderboardRabbitTopology.QUEUE)
                .deadLetterExchange(LeaderboardRabbitTopology.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(LeaderboardRabbitTopology.DEAD_LETTER_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue leaderboardXpDeadLetterQueue() {
        return QueueBuilder.durable(LeaderboardRabbitTopology.DEAD_LETTER_QUEUE).build();
    }

    @Bean
    Binding leaderboardXpBinding(Queue leaderboardXpQueue, DirectExchange contentEventsExchange) {
        return BindingBuilder.bind(leaderboardXpQueue)
                .to(contentEventsExchange)
                .with(LeaderboardRabbitTopology.XP_CHANGED_ROUTING_KEY);
    }

    @Bean
    Binding leaderboardXpDeadLetterBinding(
            Queue leaderboardXpDeadLetterQueue,
            DirectExchange contentDeadLetterExchange
    ) {
        return BindingBuilder.bind(leaderboardXpDeadLetterQueue)
                .to(contentDeadLetterExchange)
                .with(LeaderboardRabbitTopology.DEAD_LETTER_ROUTING_KEY);
    }
}
