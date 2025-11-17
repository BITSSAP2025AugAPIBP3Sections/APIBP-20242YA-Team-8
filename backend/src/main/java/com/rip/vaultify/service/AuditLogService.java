package com.rip.vaultify.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rip.vaultify.model.AuditLog;
import com.rip.vaultify.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = new ObjectMapper();
    }

    @Transactional
    public void logFileUpload(Long userId, Long fileId, String fileName, Long fileSize) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("fileName", fileName);
        metadata.put("fileSize", fileSize);
        log(userId, fileId, "FILE_UPLOAD", metadata);
    }

    @Transactional
    public void logFileDownload(Long userId, Long fileId, String fileName) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("fileName", fileName);
        log(userId, fileId, "FILE_DOWNLOAD", metadata);
    }

    @Transactional
    public void logFileShared(Long userId, Long fileId, Long sharedWithUserId, String permission) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("sharedWithUserId", sharedWithUserId);
        metadata.put("permission", permission);
        log(userId, fileId, "FILE_SHARED", metadata);
    }

    @Transactional
    public void logPermissionChanged(Long userId, Long fileId, Long targetUserId, String oldPermission, String newPermission) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("targetUserId", targetUserId);
        metadata.put("oldPermission", oldPermission);
        metadata.put("newPermission", newPermission);
        log(userId, fileId, "PERMISSION_CHANGED", metadata);
    }

    @Transactional
    public void logFileDeleted(Long userId, Long fileId, String fileName) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("fileName", fileName);
        log(userId, fileId, "FILE_DELETED", metadata);
    }

    private void log(Long userId, Long fileId, String action, Map<String, Object> metadata) {
        try {
            String metadataJson = objectMapper.writeValueAsString(metadata);
            AuditLog auditLog = new AuditLog(userId, fileId, action, metadataJson);
            auditLogRepository.save(auditLog);
        } catch (JsonProcessingException e) {
            // Log error but don't fail the operation
            System.err.println("Failed to serialize audit log metadata: " + e.getMessage());
        }
    }

    public List<AuditLog> getAuditLogsByUser(Long userId) {
        return auditLogRepository.findByUserIdOrderByTimestampDesc(userId);
    }

    public List<AuditLog> getAuditLogsByFile(Long fileId) {
        return auditLogRepository.findByFileIdOrderByTimestampDesc(fileId);
    }
}

