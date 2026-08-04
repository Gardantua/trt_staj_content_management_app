package com.trt.contentengagement.shared.ratelimit;

import java.io.IOException;

import com.trt.contentengagement.shared.api.ApiErrorResponseWriter;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@ConditionalOnProperty(
        name = "app.rate-limit.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class ApiRateLimitFilter extends OncePerRequestFilter {
    private final TokenBucketRateLimiter rateLimiter;
    private final ApiErrorResponseWriter errorResponseWriter;
    private final Counter rejectedCounter;
    private final int capacity;

    public ApiRateLimitFilter(
            TokenBucketRateLimiter rateLimiter,
            ApiErrorResponseWriter errorResponseWriter,
            MeterRegistry meterRegistry,
            RateLimitProperties properties
    ) {
        this.rateLimiter = rateLimiter;
        this.errorResponseWriter = errorResponseWriter;
        this.rejectedCounter = meterRegistry.counter("http.rate.limit.rejected");
        this.capacity = properties.getCapacity();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        TokenBucketRateLimiter.Decision decision = rateLimiter.tryAcquire(clientKey(request));
        response.setHeader("RateLimit-Limit", Integer.toString(capacity));
        response.setHeader("RateLimit-Remaining", Integer.toString(decision.remainingTokens()));

        if (!decision.allowed()) {
            rejectedCounter.increment();
            response.setHeader("Retry-After", Long.toString(decision.retryAfterSeconds()));
            errorResponseWriter.write(
                    response,
                    HttpStatus.TOO_MANY_REQUESTS.value(),
                    "RATE_LIMIT_EXCEEDED",
                    "Too many requests. Retry after the indicated delay."
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String clientKey(HttpServletRequest request) {
        String remoteAddress = request.getRemoteAddr();
        return remoteAddress == null || remoteAddress.isBlank() ? "unknown" : remoteAddress;
    }
}
