package com.trt.contentengagement.shared.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

class TokenBucketRateLimiterTest {

    @Test
    void defaultQuotaSupportsAdminWorkflowBurst() {
        RateLimitProperties properties = new RateLimitProperties();

        assertThat(properties.getCapacity()).isEqualTo(300);
        assertThat(properties.getRefillTokens()).isEqualTo(300);
        assertThat(properties.getRefillPeriod()).isEqualTo(Duration.ofMinutes(1));
    }

    @Test
    void burstIsAcceptedThenTokensRefillWithMonotonicTime() {
        AtomicLong nanoTime = new AtomicLong();
        RateLimitProperties properties = properties(2, 2, Duration.ofSeconds(10));
        TokenBucketRateLimiter rateLimiter = new TokenBucketRateLimiter(
                properties, nanoTime::get
        );

        assertThat(rateLimiter.tryAcquire("client-a").allowed()).isTrue();
        assertThat(rateLimiter.tryAcquire("client-a").allowed()).isTrue();
        TokenBucketRateLimiter.Decision rejected = rateLimiter.tryAcquire("client-a");

        assertThat(rejected.allowed()).isFalse();
        assertThat(rejected.retryAfterSeconds()).isEqualTo(5);

        nanoTime.addAndGet(Duration.ofSeconds(5).toNanos());
        assertThat(rateLimiter.tryAcquire("client-a").allowed()).isTrue();
    }

    @Test
    void differentClientsDoNotConsumeEachOthersQuota() {
        RateLimitProperties properties = properties(1, 1, Duration.ofMinutes(1));
        TokenBucketRateLimiter rateLimiter = new TokenBucketRateLimiter(properties, () -> 0L);

        assertThat(rateLimiter.tryAcquire("client-a").allowed()).isTrue();
        assertThat(rateLimiter.tryAcquire("client-a").allowed()).isFalse();
        assertThat(rateLimiter.tryAcquire("client-b").allowed()).isTrue();
    }

    @Test
    void clientsBeyondTrackingBoundShareOverflowQuota() {
        RateLimitProperties properties = properties(1, 1, Duration.ofMinutes(1));
        properties.setMaxTrackedClients(1);
        TokenBucketRateLimiter rateLimiter = new TokenBucketRateLimiter(properties, () -> 0L);

        assertThat(rateLimiter.tryAcquire("tracked-client").allowed()).isTrue();
        assertThat(rateLimiter.tryAcquire("overflow-client-a").allowed()).isTrue();
        assertThat(rateLimiter.tryAcquire("overflow-client-b").allowed()).isFalse();
    }

    private RateLimitProperties properties(
            int capacity,
            int refillTokens,
            Duration refillPeriod
    ) {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setCapacity(capacity);
        properties.setRefillTokens(refillTokens);
        properties.setRefillPeriod(refillPeriod);
        properties.setMaxTrackedClients(100);
        return properties;
    }
}
