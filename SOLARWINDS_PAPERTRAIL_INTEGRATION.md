# SolarWinds Papertrail Logging Integration

## Overview

This document describes the complete integration of SolarWinds Papertrail logging into the Vaultify application. The implementation provides structured JSON logging with correlation IDs, security audit trails, and real-time log streaming to SolarWinds cloud infrastructure.

## Features Implemented

### ✅ Core Logging Infrastructure
- **Custom HTTP Appender**: Direct integration with SolarWinds HTTP API
- **Async Logging**: Non-blocking log transmission with configurable queues
- **JSON Structured Logging**: Machine-readable logs with consistent formatting
- **Correlation IDs**: Request tracing across distributed components
- **Environment-Specific Configuration**: Different settings for dev/test/prod

### ✅ Security and Audit Logging
- **Authentication Events**: Login attempts, failures, and successes
- **Authorization Events**: Permission checks and access denials
- **File Operations**: Upload, download, delete, and copy operations
- **User Context**: Automatic user ID and session tracking
- **Security Markers**: Special markers for security-related events

### ✅ Contextual Information
- **Request Context**: HTTP method, URI, client IP, user agent
- **User Context**: User ID, username, roles
- **Operation Context**: File IDs, folder IDs, file names, sizes
- **Performance Context**: Operation timing and success/failure rates

## Configuration Files

### 1. Dependencies (`pom.xml`)
```xml
<!-- HTTP Client for SolarWinds logging -->
<dependency>
    <groupId>org.apache.httpcomponents.client5</groupId>
    <artifactId>httpclient5</artifactId>
</dependency>

<!-- JSON processing for structured logging -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>

<!-- Logback JSON encoder -->
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

### 2. Application Properties (`application.properties`)
```properties
# SolarWinds Logging Configuration
solarwinds.endpoint=https://logs.collector.ap-01.cloud.solarwinds.com/v1/logs
solarwinds.token=${SOLARWINDS_TOKEN:your-token-here}
solarwinds.application.name=vaultify
solarwinds.enabled=${SOLARWINDS_ENABLED:true}

# Logging configuration
logging.level.com.rip.vaultify=DEBUG
logging.pattern.console=%d{HH:mm:ss.SSS} [%thread] %-5level [%X{correlationId:-}] %logger{36} - %msg%n
```

### 3. Logback Configuration (`logback-spring.xml`)
- **Development Profile**: Console logging with JSON format
- **Production Profile**: Console + SolarWinds with async processing
- **Test Profile**: Flexible configuration for testing

## Implementation Components

### 1. SolarWindsHttpAppender
**Location**: `src/main/java/com/rip/vaultify/config/SolarWindsHttpAppender.java`

**Features**:
- Async HTTP client for non-blocking log transmission
- JSON formatting with contextual metadata
- Error handling and retry logic
- Configurable queue sizes and flush intervals
- Thread-safe implementation

### 2. LoggingConfig
**Location**: `src/main/java/com/rip/vaultify/config/LoggingConfig.java`

**Features**:
- Correlation ID filter for request tracing
- MDC (Mapped Diagnostic Context) management
- Client IP extraction with proxy support
- Request context automation

### 3. Enhanced Controllers
**Enhanced Files**:
- `AuthController.java`: Authentication and authorization logging
- `FileController.java`: File operation logging with security context

**Logging Patterns**:
- Security events with appropriate markers
- Contextual information (user ID, file ID, operation type)
- Error handling with detailed error information
- Performance tracking for critical operations

## Environment Configuration

### Development Environment
```bash
# No SolarWinds token needed - logs to console only
export SPRING_PROFILES_ACTIVE=dev
```

### Testing Environment
```bash
# Optional SolarWinds integration for testing
export SPRING_PROFILES_ACTIVE=test
export SOLARWINDS_TOKEN=your-test-token
```

### Production Environment
```bash
# Full SolarWinds integration enabled
export SPRING_PROFILES_ACTIVE=prod
export SOLARWINDS_TOKEN=your-production-token
export SOLARWINDS_ENABLED=true
```

## Log Format Examples

### Authentication Success
```json
{
  "timestamp": "2024-11-21T10:05:13.025Z",
  "level": "INFO",
  "logger": "com.rip.vaultify.controller.AuthController",
  "message": "Login successful for username: testuser, roles: [ROLE_USER]",
  "application": "vaultify",
  "thread": "http-nio-8080-exec-3",
  "mdc": {
    "correlationId": "863abaed-6aeb-4ace-8e87-5e601541f373",
    "userId": "testuser",
    "action": "login",
    "targetUsername": "testuser",
    "roles": "ROLE_USER",
    "requestUri": "/auth/login",
    "httpMethod": "POST",
    "clientIp": "127.0.0.1"
  },
  "marker": "AUTH_SUCCESS"
}
```

### File Upload
```json
{
  "timestamp": "2024-11-21T10:05:15.123Z",
  "level": "INFO",
  "logger": "com.rip.vaultify.controller.FileController",
  "message": "File upload successful - fileId: 1, fileName: document.pdf, size: 2048576 bytes, user: 1",
  "application": "vaultify",
  "thread": "http-nio-8080-exec-5",
  "mdc": {
    "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "userId": "1",
    "action": "file_upload",
    "fileId": "1",
    "fileName": "document.pdf",
    "fileSize": "2048576",
    "folderId": "2",
    "contentType": "application/pdf"
  },
  "marker": "AUDIT"
}
```

### Security Alert
```json
{
  "timestamp": "2024-11-21T10:05:16.184Z",
  "level": "WARN",
  "logger": "com.rip.vaultify.controller.AuthController",
  "message": "Login failed - invalid credentials for username: testuser",
  "application": "vaultify",
  "thread": "http-nio-8080-exec-7",
  "mdc": {
    "correlationId": "133f76af-2ad8-4f1e-892f-79a81a4f171a",
    "action": "login",
    "targetUsername": "testuser",
    "requestUri": "/auth/login",
    "httpMethod": "POST",
    "clientIp": "127.0.0.1"
  },
  "marker": "AUTH_FAILURE"
}
```

## Testing and Verification

### 1. Run the Test Script
```bash
chmod +x backend/test-logging.sh
./backend/test-logging.sh
```

### 2. Check Application Logs
- Monitor console output for structured logs
- Verify correlation IDs are present
- Confirm security events are logged

### 3. Verify SolarWinds Integration
- Check SolarWinds dashboard for incoming logs
- Search for application logs using `application:vaultify`
- Verify JSON parsing and field extraction

## Monitoring and Alerting

### Recommended SolarWinds Searches

**Security Events**:
```
application:vaultify AND marker:(AUTH_FAILURE OR SECURITY)
```

**File Operations**:
```
application:vaultify AND marker:FILE_OPERATION
```

**Error Events**:
```
application:vaultify AND level:ERROR
```

**User Activity**:
```
application:vaultify AND mdc.userId:* AND marker:AUDIT
```

### Alerting Rules

1. **Failed Login Attempts**: Alert on multiple AUTH_FAILURE events
2. **File Access Violations**: Alert on SECURITY marker events
3. **Application Errors**: Alert on ERROR level logs
4. **High Volume Operations**: Alert on unusual file operation volumes

## Performance Considerations

### Async Logging Configuration
- **Queue Size**: 1000-2000 events (configurable)
- **Flush Interval**: 3-5 seconds
- **Batch Size**: 10-50 events per batch
- **Max Flush Time**: 5 seconds

### Network Resilience
- **Connection Pooling**: HTTP client connection reuse
- **Retry Logic**: Automatic retry on network failures
- **Graceful Degradation**: Continue operation if logging fails
- **Circuit Breaker**: Optional circuit breaker for logging failures

## Security Considerations

### Token Management
- Store SolarWinds token in environment variables
- Use different tokens for different environments
- Rotate tokens regularly
- Monitor token usage in SolarWinds

### Data Privacy
- Avoid logging sensitive data (passwords, tokens, personal data)
- Use structured logging to control data exposure
- Implement log retention policies
- Consider data encryption in transit

## Troubleshooting

### Common Issues

1. **Logs not appearing in SolarWinds**
   - Check token configuration
   - Verify network connectivity
   - Check application startup logs for appender errors

2. **Performance Issues**
   - Adjust queue sizes
   - Increase flush intervals
   - Monitor memory usage

3. **Missing Context Information**
   - Verify filter registration
   - Check MDC cleanup in finally blocks
   - Ensure proper thread context

### Debug Mode
Enable debug logging to troubleshoot issues:
```properties
logging.level.com.rip.vaultify.config.SolarWindsHttpAppender=DEBUG
```

## Maintenance

### Regular Tasks
- Monitor log volume and costs
- Review and update log retention policies
- Update dependencies regularly
- Test failover scenarios
- Review security and audit logs

### Configuration Updates
- Adjust log levels based on environment needs
- Update correlation ID formats if needed
- Modify JSON structure for new requirements
- Update alerting rules based on operational experience

## Conclusion

The SolarWinds Papertrail integration provides comprehensive logging capabilities for the Vaultify application with:

- **Real-time log streaming** to cloud infrastructure
- **Structured JSON logs** for efficient searching and analysis
- **Security audit trails** for compliance and monitoring
- **Performance monitoring** capabilities
- **Operational visibility** into application behavior

This implementation follows best practices for production logging systems and provides a solid foundation for monitoring, debugging, and security analysis.
