package com.Mediscan.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    @Value("${rate.limit.prices.requests-per-minute:10}")
    private int priceRateLimit;

    @Value("${rate.limit.identify.requests-per-minute:5}")
    private int identifyRateLimit;

    @Value("${rate.limit.generics.requests-per-minute:15}")
    private int genericsRateLimit;

    // One bucket per client IP address
    private final Map<String, Bucket> priceBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> identifyBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> genericsBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String clientIp = getClientIp(request);

        // Only rate-limit specific API paths
        if (path.startsWith("/api/v1/prices")) {
            Bucket bucket = priceBuckets.computeIfAbsent(clientIp,
                    k -> createBucket(priceRateLimit));
            if (!bucket.tryConsume(1)) {
                log.warn("Rate limit exceeded for IP {} on prices endpoint", clientIp);
                sendRateLimitResponse(response);
                return;
            }
        } else if (path.startsWith("/api/v1/medicine/identify")) {
            Bucket bucket = identifyBuckets.computeIfAbsent(clientIp,
                    k -> createBucket(identifyRateLimit));

            if (!bucket.tryConsume(1)) {
                log.warn("Rate limit exceeded for IP {} on identify endpoint", clientIp);
                sendRateLimitResponse(response);
                return;
            }
        } else if (path.startsWith("/api/v1/generics")) {
            Bucket bucket = genericsBuckets.computeIfAbsent(clientIp,
                    k -> createBucket(genericsRateLimit));

            if (!bucket.tryConsume(1)) {
                log.warn("Rate limit exceeded for IP {} on generics endpoint", clientIp);
                sendRateLimitResponse(response);
                return;
            }
        }

        

        // Request is allowed — continue to controller
        filterChain.doFilter(request, response);
    }

    /**
     * Creates a token bucket with the specified requests-per-minute limit.
     * Uses "greedy" refill — tokens are added smoothly over time, not all at once.
     */
    private Bucket createBucket(int requestsPerMinute) {
        Bandwidth limit = Bandwidth.classic(
                requestsPerMinute,
                Refill.greedy(requestsPerMinute, Duration.ofMinutes(1))
        );
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * Sends a standardized 429 Too Many Requests response.
     */
    private void sendRateLimitResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setHeader("Retry-After", "60");
        response.getWriter().write("""
                {
                    "error": "Rate limit exceeded. Please try again later.",
                    "status": 429,
                    "retryAfterSeconds": 60
                }
                """);
    }

    /**
     * Extracts the real client IP, handling reverse proxies.
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

