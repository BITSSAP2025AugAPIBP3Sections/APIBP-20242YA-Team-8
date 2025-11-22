package com.rip.vaultify.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.rip.vaultify.config.RateLimitConfig;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitConfig rateLimitConfig;
    private final Cache<String, Bucket> rateLimitCache;
    private ObjectMapper objectMapper;

    @Autowired
    public RateLimitFilter(RateLimitConfig rateLimitConfig, Cache<String, Bucket> rateLimitCache) {
        this.rateLimitConfig = rateLimitConfig;
        this.rateLimitCache = rateLimitCache;
    }

    @Autowired(required = false)
    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String path = request.getRequestURI();
        String clientKey = getClientKey(request);
        
        Bucket bucket = null;
        String rateLimitType = null;

        // Determine rate limit based on endpoint
        if (path.startsWith("/auth/login") || path.startsWith("/auth/register")) {
            bucket = rateLimitConfig.getAuthBucket(rateLimitCache, clientKey);
            rateLimitType = "authentication";
        } else if (path.startsWith("/api/files/upload")) {
            bucket = rateLimitConfig.getUploadBucket(rateLimitCache, clientKey);
            rateLimitType = "upload";
        } else if (path.startsWith("/api/") || path.startsWith("/graphql")) {
            bucket = rateLimitConfig.getApiBucket(rateLimitCache, clientKey);
            rateLimitType = "api";
        }

        // If rate limiting applies, check the bucket
        if (bucket != null) {
            if (!bucket.tryConsume(1)) {
                // Rate limit exceeded
                handleRateLimitExceeded(response, rateLimitType, bucket);
                return;
            }
            
            // Add rate limit headers
            addRateLimitHeaders(response, bucket, rateLimitType);
        }

        filterChain.doFilter(request, response);
    }

    private String getClientKey(HttpServletRequest request) {
        // Try to get authenticated user first
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && 
            !authentication.getPrincipal().equals("anonymousUser")) {
            return authentication.getName();
        }
        
        // Fall back to IP address
        String ipAddress = getClientIpAddress(request);
        return ipAddress;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    private void handleRateLimitExceeded(HttpServletResponse response, String rateLimitType, Bucket bucket) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Rate limit exceeded");
        errorResponse.put("message", String.format("Too many %s requests. Please try again later.", rateLimitType));
        errorResponse.put("retryAfter", getRetryAfterSeconds(bucket));
        
        // Add rate limit headers
        addRateLimitHeaders(response, bucket, rateLimitType);
        
        // Use ObjectMapper if available, otherwise write JSON manually
        if (objectMapper != null) {
            objectMapper.writeValue(response.getWriter(), errorResponse);
        } else {
            // Fallback: write JSON manually
            String json = String.format(
                "{\"error\":\"Rate limit exceeded\",\"message\":\"Too many %s requests. Please try again later.\",\"retryAfter\":%d}",
                rateLimitType, getRetryAfterSeconds(bucket)
            );
            response.getWriter().write(json);
        }
    }

    private void addRateLimitHeaders(HttpServletResponse response, Bucket bucket, String rateLimitType) {
        long availableTokens = bucket.getAvailableTokens();
        long capacity = getCapacityForType(rateLimitType);
        long windowSeconds = getWindowSecondsForType(rateLimitType);
        
        response.setHeader("X-RateLimit-Limit", String.valueOf(capacity));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(availableTokens));
        response.setHeader("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() / 1000 + windowSeconds));
        response.setHeader("X-RateLimit-Type", rateLimitType);
    }

    private long getCapacityForType(String rateLimitType) {
        return switch (rateLimitType) {
            case "authentication" -> rateLimitConfig.getAuthRequests();
            case "upload" -> rateLimitConfig.getUploadRequests();
            case "api" -> rateLimitConfig.getApiRequests();
            default -> 100;
        };
    }

    private long getWindowSecondsForType(String rateLimitType) {
        return switch (rateLimitType) {
            case "authentication" -> rateLimitConfig.getAuthWindowSeconds();
            case "upload" -> rateLimitConfig.getUploadWindowSeconds();
            case "api" -> rateLimitConfig.getApiWindowSeconds();
            default -> 60;
        };
    }

    private long getRetryAfterSeconds(Bucket bucket) {
        // Calculate retry after based on bucket's available tokens
        long availableTokens = bucket.getAvailableTokens();
        if (availableTokens > 0) {
            return 0; // Can retry immediately
        }
        // Estimate based on window - tokens will refill after the window period
        return 60; // Default to 60 seconds
    }
}
