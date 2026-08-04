package com.trt.contentengagement.shared.ratelimit;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

public class TokenBucketRateLimiter {
    private static final String OVERFLOW_CLIENT_KEY = "__overflow__";
    private static final long CLEANUP_INTERVAL = 1_024;

    private final int capacity;
    private final double tokensPerNanosecond;
    private final long idleExpirationNanoseconds;
    private final int maxTrackedClients;
    private final LongSupplier nanoTime;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final AtomicLong requestCounter = new AtomicLong();

    public TokenBucketRateLimiter(RateLimitProperties properties) {
        this(properties, System::nanoTime);
    }

    TokenBucketRateLimiter(RateLimitProperties properties, LongSupplier nanoTime) {
        validate(properties);
        this.capacity = properties.getCapacity();
        this.tokensPerNanosecond = (double) properties.getRefillTokens()
                / properties.getRefillPeriod().toNanos();
        this.idleExpirationNanoseconds = multiplyWithoutOverflow(
                properties.getRefillPeriod().toNanos(), 2
        );
        this.maxTrackedClients = properties.getMaxTrackedClients();
        this.nanoTime = nanoTime;
    }

    public Decision tryAcquire(String clientKey) {
        long now = nanoTime.getAsLong();
        cleanupIfRequired(now);
        Bucket bucket = bucketFor(clientKey, now);
        return bucket.tryAcquire(now, capacity, tokensPerNanosecond);
    }

    private Bucket bucketFor(String clientKey, long now) {
        synchronized (buckets) {
            Bucket existingBucket = buckets.get(clientKey);
            if (existingBucket != null) {
                return existingBucket;
            }
            String boundedClientKey = buckets.size() < maxTrackedClients
                    ? clientKey
                    : OVERFLOW_CLIENT_KEY;
            return buckets.computeIfAbsent(
                    boundedClientKey,
                    ignored -> new Bucket(capacity, now)
            );
        }
    }

    private void cleanupIfRequired(long now) {
        if (requestCounter.incrementAndGet() % CLEANUP_INTERVAL != 0) {
            return;
        }
        buckets.entrySet().removeIf(entry ->
                !OVERFLOW_CLIENT_KEY.equals(entry.getKey())
                        && now - entry.getValue().lastSeenNanoseconds
                        > idleExpirationNanoseconds
        );
    }

    private static void validate(RateLimitProperties properties) {
        if (properties.getCapacity() <= 0
                || properties.getRefillTokens() <= 0
                || properties.getMaxTrackedClients() <= 0
                || properties.getRefillPeriod() == null
                || properties.getRefillPeriod().isZero()
                || properties.getRefillPeriod().isNegative()) {
            throw new IllegalArgumentException("Rate limit values must be positive.");
        }
    }

    private static long multiplyWithoutOverflow(long value, int multiplier) {
        if (value > Long.MAX_VALUE / multiplier) {
            return Long.MAX_VALUE;
        }
        return value * multiplier;
    }

    public record Decision(boolean allowed, int remainingTokens, long retryAfterSeconds) {
    }

    private static final class Bucket {
        private double availableTokens;
        private long lastRefillNanoseconds;
        private volatile long lastSeenNanoseconds;

        private Bucket(int capacity, long now) {
            this.availableTokens = capacity;
            this.lastRefillNanoseconds = now;
            this.lastSeenNanoseconds = now;
        }

        private synchronized Decision tryAcquire(
                long now,
                int capacity,
                double tokensPerNanosecond
        ) {
            long elapsedNanoseconds = Math.max(0, now - lastRefillNanoseconds);
            availableTokens = Math.min(
                    capacity,
                    availableTokens + elapsedNanoseconds * tokensPerNanosecond
            );
            lastRefillNanoseconds = now;
            lastSeenNanoseconds = now;

            if (availableTokens >= 1) {
                availableTokens -= 1;
                return new Decision(true, (int) Math.floor(availableTokens), 0);
            }

            long retryNanoseconds = (long) Math.ceil(
                    (1 - availableTokens) / tokensPerNanosecond
            );
            long retrySeconds = Math.max(
                    1,
                    Duration.ofNanos(retryNanoseconds).toSeconds()
                            + (retryNanoseconds % 1_000_000_000L == 0 ? 0 : 1)
            );
            return new Decision(false, 0, retrySeconds);
        }
    }
}
