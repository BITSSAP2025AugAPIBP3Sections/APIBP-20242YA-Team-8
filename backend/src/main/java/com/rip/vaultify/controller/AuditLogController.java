package com.rip.vaultify.controller;

import com.rip.vaultify.model.AuditLog;
import com.rip.vaultify.service.AuditLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AuditLog>> getAuditLogsByUser(@PathVariable Long userId) {
        // Optional: Verify user has permission to view these logs
        // For now, we'll allow any authenticated user to view any user's logs
        // In production, you'd want to restrict this
        List<AuditLog> logs = auditLogService.getAuditLogsByUser(userId);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/file/{fileId}")
    public ResponseEntity<List<AuditLog>> getAuditLogsByFile(@PathVariable Long fileId) {
        // Optional: Verify user has access to this file
        List<AuditLog> logs = auditLogService.getAuditLogsByFile(fileId);
        return ResponseEntity.ok(logs);
    }
}

