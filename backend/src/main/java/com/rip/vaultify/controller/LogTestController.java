package com.rip.vaultify.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class LogTestController {

    private static final Logger logger = LoggerFactory.getLogger(LogTestController.class);

    @GetMapping("/logs")
    public ResponseEntity<Map<String, String>> testLogs(@RequestParam(defaultValue = "info") String level) {
        
        // Set some MDC context for structured logging
        MDC.put("userId", "test-user-123");
        MDC.put("requestId", "req-" + System.currentTimeMillis());
        MDC.put("feature", "logging-test");
        
        try {
            Map<String, String> response = new HashMap<>();
            
            // Generate different log levels based on parameter
            switch (level.toLowerCase()) {
                case "debug":
                    logger.debug("🔍 Debug log: Testing SolarWinds integration with debug level");
                    response.put("level", "DEBUG");
                    break;
                case "info":
                    logger.info("ℹ️ Info log: SolarWinds logging integration test successful");
                    response.put("level", "INFO");
                    break;
                case "warn":
                    logger.warn("⚠️ Warning log: This is a test warning for SolarWinds");
                    response.put("level", "WARN");
                    break;
                case "error":
                    logger.error("❌ Error log: Test error for SolarWinds logging (not a real error)");
                    response.put("level", "ERROR");
                    break;
                case "all":
                    logger.debug("🔍 Debug: Testing all log levels for SolarWinds");
                    logger.info("ℹ️ Info: All log levels test - SolarWinds integration");
                    logger.warn("⚠️ Warning: Test warning - SolarWinds integration");
                    logger.error("❌ Error: Test error - SolarWinds integration (not real)");
                    response.put("level", "ALL");
                    break;
                default:
                    logger.info("ℹ️ Default info log: Unknown level '{}', using INFO", level);
                    response.put("level", "INFO (default)");
            }
            
            // Log structured data
            logger.info("📊 Structured log test - User action recorded", 
                org.slf4j.helpers.MessageFormatter.arrayFormat(
                    "action=test_logs, level={}, timestamp={}, success=true", 
                    new Object[]{level, System.currentTimeMillis()}
                ).getMessage());
                
            response.put("message", "Test logs generated successfully");
            response.put("timestamp", String.valueOf(System.currentTimeMillis()));
            response.put("solarwinds", "enabled");
            
            return ResponseEntity.ok(response);
            
        } finally {
            // Clean up MDC
            MDC.clear();
        }
    }

    @GetMapping("/error-simulation")
    public ResponseEntity<Map<String, String>> simulateError() {
        MDC.put("userId", "error-test-user");
        MDC.put("requestId", "error-req-" + System.currentTimeMillis());
        MDC.put("feature", "error-simulation");
        
        try {
            logger.error("🚨 Simulated application error for testing SolarWinds error tracking");
            logger.error("Stack trace simulation - SolarWinds error monitoring test", 
                new RuntimeException("Test exception for SolarWinds (not a real error)"));
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Error simulation completed");
            response.put("logs_sent", "true");
            response.put("solarwinds", "error logged");
            
            return ResponseEntity.ok(response);
            
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/audit")
    public ResponseEntity<Map<String, String>> auditLog() {
        MDC.put("userId", "audit-user-456");
        MDC.put("requestId", "audit-req-" + System.currentTimeMillis());
        MDC.put("feature", "security-audit");
        MDC.put("action", "file_access");
        MDC.put("resource", "test-document.pdf");
        
        try {
            // Simulate security audit log
            logger.info("🔐 AUDIT: User accessed file - Testing SolarWinds security monitoring");
            logger.warn("🚨 SECURITY: Potential suspicious activity detected (test only)");
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Audit logs generated");
            response.put("security_event", "logged");
            response.put("solarwinds", "audit trail sent");
            
            return ResponseEntity.ok(response);
            
        } finally {
            MDC.clear();
        }
    }
}
