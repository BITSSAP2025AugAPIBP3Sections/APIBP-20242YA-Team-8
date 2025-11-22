package com.rip.vaultify.config;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.Layout;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Method;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Custom Logback appender for sending logs to SolarWinds via HTTP API
 */
public class SolarWindsHttpAppender extends AppenderBase<ILoggingEvent> {

    private String endpoint;
    private String token;
    private String applicationName = "vaultify";
    private Layout<ILoggingEvent> layout;
    private int queueSize = 1000;
    private int batchSize = 10;
    private int flushIntervalMs = 5000;

    private CloseableHttpAsyncClient httpClient;
    private BlockingQueue<ILoggingEvent> eventQueue;
    private Thread workerThread;
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void start() {
        if (endpoint == null || endpoint.trim().isEmpty()) {
            addError("SolarWinds endpoint is not configured");
            return;
        }
        if (token == null || token.trim().isEmpty()) {
            addError("SolarWinds token is not configured");
            return;
        }

        eventQueue = new LinkedBlockingQueue<>(queueSize);
        httpClient = HttpAsyncClients.createDefault();
        httpClient.start();

        // Start worker thread for async processing
        workerThread = new Thread(this::processEvents, "SolarWinds-Logger");
        workerThread.setDaemon(true);
        workerThread.start();

        super.start();
        addInfo("SolarWinds HTTP Appender started with endpoint: " + endpoint);
    }

    @Override
    public void stop() {
        shutdown.set(true);
        
        if (workerThread != null) {
            workerThread.interrupt();
            try {
                workerThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (httpClient != null) {
            try {
                httpClient.close();
            } catch (Exception e) {
                addError("Error closing HTTP client", e);
            }
        }

        super.stop();
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (!isStarted() || shutdown.get()) {
            return;
        }

        // Try to add event to queue, drop if queue is full
        if (!eventQueue.offer(event)) {
            addWarn("Event queue is full, dropping log event");
        }
    }

    private void processEvents() {
        while (!shutdown.get()) {
            try {
                ILoggingEvent event = eventQueue.poll(flushIntervalMs, TimeUnit.MILLISECONDS);
                if (event != null) {
                    sendLogEvent(event);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                addError("Error processing log events", e);
            }
        }

        // Process remaining events before shutdown
        ILoggingEvent event;
        while ((event = eventQueue.poll()) != null) {
            try {
                sendLogEvent(event);
            } catch (Exception e) {
                addError("Error processing remaining log events during shutdown", e);
            }
        }
    }

    private void sendLogEvent(ILoggingEvent event) {
        try {
            String logMessage = formatLogEvent(event);
            
            URI uri = URI.create(endpoint);
            SimpleHttpRequest request = SimpleHttpRequest.create("POST", uri);
            request.setHeader("Authorization", "Bearer " + token);
            request.setHeader("Content-Type", "application/json");
            request.setBody(logMessage, ContentType.APPLICATION_JSON);

            httpClient.execute(request, new FutureCallback<SimpleHttpResponse>() {
                @Override
                public void completed(SimpleHttpResponse response) {
                    if (response.getCode() >= 400) {
                        addWarn("Failed to send log to SolarWinds. Status: " + response.getCode() + 
                               ", Body: " + response.getBodyText());
                    }
                }

                @Override
                public void failed(Exception ex) {
                    addError("Failed to send log to SolarWinds", ex);
                }

                @Override
                public void cancelled() {
                    addWarn("Log request to SolarWinds was cancelled");
                }
            });

        } catch (Exception e) {
            addError("Error sending log event to SolarWinds", e);
        }
    }

    private String formatLogEvent(ILoggingEvent event) throws Exception {
        Map<String, Object> logData = new HashMap<>();
        
        // Basic log information
        logData.put("timestamp", Instant.ofEpochMilli(event.getTimeStamp()).toString());
        logData.put("level", event.getLevel().toString());
        logData.put("logger", event.getLoggerName());
        logData.put("message", event.getFormattedMessage());
        logData.put("application", applicationName);
        logData.put("thread", event.getThreadName());

        // Add MDC properties if available
        if (event.getMDCPropertyMap() != null && !event.getMDCPropertyMap().isEmpty()) {
            logData.put("mdc", event.getMDCPropertyMap());
        }

        // Add exception information if present
        if (event.getThrowableProxy() != null) {
            Map<String, Object> exception = new HashMap<>();
            exception.put("class", event.getThrowableProxy().getClassName());
            exception.put("message", event.getThrowableProxy().getMessage());
            exception.put("stackTrace", event.getThrowableProxy().getStackTraceElementProxyArray());
            logData.put("exception", exception);
        }

        // Add marker if present
        if (event.getMarker() != null) {
            logData.put("marker", event.getMarker().getName());
        }

        return objectMapper.writeValueAsString(logData);
    }

    // Getters and setters for configuration
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public void setLayout(Layout<ILoggingEvent> layout) {
        this.layout = layout;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public void setFlushIntervalMs(int flushIntervalMs) {
        this.flushIntervalMs = flushIntervalMs;
    }
}
