package com.rip.vaultify.service;

import com.rip.vaultify.dto.PreSignedUrlRequest;
import com.rip.vaultify.dto.PreSignedUrlResponse;
import com.rip.vaultify.model.File;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class PreSignedUrlService {

    private final FileService fileService;
    private final FolderService folderService;
    
    // In-memory storage for presigned URL tokens
    private final Map<String, TokenEntry> tokenStore = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupScheduler = Executors.newScheduledThreadPool(1);

    private static final long PRESIGNED_URL_TTL_SECONDS = 60;

    @Value("${server.port:8080}")
    private int serverPort;

    @Autowired
    public PreSignedUrlService(FileService fileService, FolderService folderService) {
        this.fileService = fileService;
        this.folderService = folderService;
        
        // Cleanup expired tokens every 30 seconds
        cleanupScheduler.scheduleAtFixedRate(this::cleanupExpiredTokens, 30, 30, TimeUnit.SECONDS);
    }
    
    private static class TokenEntry {
        final Map<String, Object> data;
        final long expiresAt;
        
        TokenEntry(Map<String, Object> data, long ttlSeconds) {
            this.data = data;
            this.expiresAt = System.currentTimeMillis() + (ttlSeconds * 1000);
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
    
    private void cleanupExpiredTokens() {
        tokenStore.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    /**
     * Generate a pre-signed URL for upload or download
     */
    public PreSignedUrlResponse generatePreSignedUrl(PreSignedUrlRequest request, Long userId) {
        String action = request.getAction();
        if (!"read".equals(action) && !"write".equals(action)) {
            throw new IllegalArgumentException("Action must be 'read' or 'write'");
        }

        Map<String, Object> tokenData = new HashMap<>();
        tokenData.put("action", action);
        tokenData.put("userId", userId);

        if ("read".equals(action)) {
            if (request.getFileId() == null) {
                throw new IllegalArgumentException("fileId is required for read operation");
            }
            File file = fileService.getFileByIdAndUser(request.getFileId(), userId);
            tokenData.put("fileId", file.getId());
            tokenData.put("folderId", file.getFolder().getId());
        } else {
            Long folderId = request.getFolderId();
            if (request.getFileId() != null) {
                File file = fileService.getFileByIdForWrite(request.getFileId(), userId);
                tokenData.put("fileId", file.getId());
                folderId = file.getFolder().getId();
            }

            if (folderId == null) {
                throw new IllegalArgumentException("folderId is required for write operation");
            }

            if (!folderService.isFolderOwner(folderId, userId)) {
                throw new RuntimeException("Folder does not belong to user");
            }

            tokenData.put("folderId", folderId);
        }

        // Generate unique token
        String token = UUID.randomUUID().toString();
        
        // Store token in memory with expiration
        tokenStore.put(token, new TokenEntry(tokenData, PRESIGNED_URL_TTL_SECONDS));

        // Generate URL
        String baseUrl = "http://localhost:" + serverPort;
        String url = baseUrl + "/api/v1/files/presign/" + action + "?token=" + token;

        return new PreSignedUrlResponse(token, url, PRESIGNED_URL_TTL_SECONDS);
    }

    /**
     * Validate and retrieve pre-signed URL token data
     */
    public Map<String, Object> validateToken(String token) {
        TokenEntry entry = tokenStore.get(token);
        
        if (entry == null || entry.isExpired()) {
            if (entry != null) {
                tokenStore.remove(token); // Clean up expired entry
            }
            throw new RuntimeException("Invalid or expired token");
        }

        return entry.data;
    }

    /**
     * Invalidate a pre-signed URL token
     */
    public void invalidateToken(String token) {
        tokenStore.remove(token);
    }
}