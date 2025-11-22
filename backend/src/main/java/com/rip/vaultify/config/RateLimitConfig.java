package com.rip.vaultify.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class RateLimitConfig {

    @Value("${ratelimit.auth.requests:5}")
    private int authRequests;

    @Value("${ratelimit.auth.window:60}")
    private int authWindowSeconds;

    @Value("${ratelimit.api.requests:100}")
    private int apiRequests;

    @Value("${ratelimit.api.window:60}")
    private int apiWindowSeconds;

    @Value("${ratelimit.upload.requests:10}")
    private int uploadRequests;

    @Value("${ratelimit.upload.window:60}")
    private int uploadWindowSeconds;

    @Bean
    public Cache<String, Bucket> rateLimitCache() {
        return Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build();
    }

    public Bucket resolveBucket(Cache<String, Bucket> cache, String key, int capacity, int windowSeconds) {
        return cache.get(key, k -> {
            Bandwidth limit = Bandwidth.classic(capacity, Refill.intervally(capacity, Duration.ofSeconds(windowSeconds)));
            return Bucket.builder()
                    .addLimit(limit)
                    .build();
        });
    }

    public Bucket getAuthBucket(Cache<String, Bucket> cache, String key) {
        return resolveBucket(cache, "auth:" + key, authRequests, authWindowSeconds);
    }

    public Bucket getApiBucket(Cache<String, Bucket> cache, String key) {
        return resolveBucket(cache, "api:" + key, apiRequests, apiWindowSeconds);
    }

    public Bucket getUploadBucket(Cache<String, Bucket> cache, String key) {
        return resolveBucket(cache, "upload:" + key, uploadRequests, uploadWindowSeconds);
    }

    // Getters for configuration values
    public int getAuthRequests() {
        return authRequests;
    }

    public int getAuthWindowSeconds() {
        return authWindowSeconds;
    }

    public int getApiRequests() {
        return apiRequests;
    }

    public int getApiWindowSeconds() {
        return apiWindowSeconds;
    }

    public int getUploadRequests() {
        return uploadRequests;
    }

    public int getUploadWindowSeconds() {
        return uploadWindowSeconds;
    }
}
