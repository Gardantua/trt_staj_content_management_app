package com.trt.contentengagement.shared.ratelimit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfiguration {

    @Bean
    @ConditionalOnProperty(
            name = "app.rate-limit.enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    TokenBucketRateLimiter tokenBucketRateLimiter(RateLimitProperties properties) {
        return new TokenBucketRateLimiter(properties);
    }
}
