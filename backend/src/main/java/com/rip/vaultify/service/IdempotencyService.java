package com.rip.vaultify.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service to handle idempotency keys
 * Prevents duplicate processing of the same request
 */
@Service
public class IdempotencyService {

    private final Map<String, IdempotencyEntry> idempotencyStore = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupScheduler = Executors.newScheduledThreadPool(1);
    
    private static final long MAX_AGE_MS = 24 * 60 * 60 * 1000; // 24 hours

    public IdempotencyService() {
        // Cleanup expired entries every hour
        cleanupScheduler.scheduleAtFixedRate(this::cleanupExpiredEntries, 1, 1, TimeUnit.HOURS);
    }

    private static class IdempotencyEntry {
        final Object response;
        final long timestamp;

        IdempotencyEntry(Object response) {
            this.response = response;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > MAX_AGE_MS;
        }
    }

    /**
     * Check if a request with this idempotency key was already processed
     */
    public <T> T getCachedResponse(String idempotencyKey, Class<T> responseType) {
        IdempotencyEntry entry = idempotencyStore.get(idempotencyKey);
        if (entry != null && !entry.isExpired()) {
            @SuppressWarnings("unchecked")
            T response = (T) entry.response;
            return response;
        }
        return null;
    }

    /**
     * Store the response for an idempotency key
     */
    public void storeResponse(String idempotencyKey, Object response) {
        idempotencyStore.put(idempotencyKey, new IdempotencyEntry(response));
    }

    /**
     * Check if idempotency key exists (without getting response)
     */
    public boolean hasKey(String idempotencyKey) {
        IdempotencyEntry entry = idempotencyStore.get(idempotencyKey);
        return entry != null && !entry.isExpired();
    }

    private void cleanupExpiredEntries() {
        idempotencyStore.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
}


