package com.rip.vaultify.service;

import com.rip.vaultify.dto.PreSignedUrlRequest;
import com.rip.vaultify.dto.PreSignedUrlResponse;
import com.rip.vaultify.model.File;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class PreSignedUrlService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final FileService fileService;

    private static final String PRESIGNED_KEY_PREFIX = "presigned:";
    private static final long PRESIGNED_URL_TTL_SECONDS = 60;

    @Value("${server.port:8080}")
    private int serverPort;

    @Autowired
    public PreSignedUrlService(RedisTemplate<String, Object> redisTemplate,
                               FileService fileService) {
        this.redisTemplate = redisTemplate;
        this.fileService = fileService;
    }

    /**
     * Generate a pre-signed URL for upload or download
     */
    public PreSignedUrlResponse generatePreSignedUrl(PreSignedUrlRequest request, Long userId) {
        // Validate file exists and user has access
        if (request.getAction().equals("read") || request.getAction().equals("write")) {
            File file = fileService.getFileByIdAndUser(request.getFileId(), userId);
            
            // Generate unique token
            String token = UUID.randomUUID().toString();
            String cacheKey = PRESIGNED_KEY_PREFIX + token;
            
            // Store token metadata in Redis
            Map<String, Object> tokenData = new HashMap<>();
            tokenData.put("fileId", file.getId());
            tokenData.put("action", request.getAction());
            tokenData.put("userId", userId);
            
            redisTemplate.opsForValue().set(cacheKey, tokenData, PRESIGNED_URL_TTL_SECONDS, TimeUnit.SECONDS);
            
            // Generate URL
            String baseUrl = "http://localhost:" + serverPort;
            String url = baseUrl + "/api/v1/files/presign/" + request.getAction() + "?token=" + token;
            
            return new PreSignedUrlResponse(token, url, PRESIGNED_URL_TTL_SECONDS);
        } else {
            throw new IllegalArgumentException("Action must be 'read' or 'write'");
        }
    }

    /**
     * Validate and retrieve pre-signed URL token data
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> validateToken(String token) {
        String cacheKey = PRESIGNED_KEY_PREFIX + token;
        Object tokenData = redisTemplate.opsForValue().get(cacheKey);
        
        if (tokenData == null) {
            throw new RuntimeException("Invalid or expired token");
        }
        
        return (Map<String, Object>) tokenData;
    }

    /**
     * Invalidate a pre-signed URL token
     */
    public void invalidateToken(String token) {
        String cacheKey = PRESIGNED_KEY_PREFIX + token;
        redisTemplate.delete(cacheKey);
    }
}

