package com.rip.vaultify.config;

import org.slf4j.MDC;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * Configuration for structured logging and correlation IDs
 */
@Configuration
public class LoggingConfig {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";
    public static final String USER_ID_MDC_KEY = "userId";
    public static final String REQUEST_ID_MDC_KEY = "requestId";
    public static final String REQUEST_URI_MDC_KEY = "requestUri";
    public static final String HTTP_METHOD_MDC_KEY = "httpMethod";
    public static final String USER_AGENT_MDC_KEY = "userAgent";
    public static final String CLIENT_IP_MDC_KEY = "clientIp";

    /**
     * Filter to add correlation ID and request context to MDC
     */
    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilter() {
        FilterRegistrationBean<CorrelationIdFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new CorrelationIdFilter());
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registrationBean;
    }

    /**
     * Servlet filter to add correlation ID and request context to logging MDC
     */
    public static class CorrelationIdFilter implements Filter {

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            try {
                // Generate or extract correlation ID
                String correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER);
                if (correlationId == null || correlationId.trim().isEmpty()) {
                    correlationId = UUID.randomUUID().toString();
                }

                // Add correlation ID to response header
                httpResponse.setHeader(CORRELATION_ID_HEADER, correlationId);

                // Set up MDC context
                MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
                MDC.put(REQUEST_ID_MDC_KEY, UUID.randomUUID().toString());
                MDC.put(REQUEST_URI_MDC_KEY, httpRequest.getRequestURI());
                MDC.put(HTTP_METHOD_MDC_KEY, httpRequest.getMethod());
                
                // Add user agent if present
                String userAgent = httpRequest.getHeader("User-Agent");
                if (userAgent != null) {
                    MDC.put(USER_AGENT_MDC_KEY, userAgent);
                }

                // Add client IP
                String clientIp = getClientIpAddress(httpRequest);
                if (clientIp != null) {
                    MDC.put(CLIENT_IP_MDC_KEY, clientIp);
                }

                chain.doFilter(request, response);
            } finally {
                // Clear MDC after request processing
                MDC.clear();
            }
        }

        /**
         * Extract client IP address considering proxy headers
         */
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
    }

    /**
     * Utility class for adding user context to logs
     */
    public static class LoggingContext {
        
        /**
         * Add user ID to MDC for all subsequent log statements in this thread
         */
        public static void setUserId(String userId) {
            if (userId != null) {
                MDC.put(USER_ID_MDC_KEY, userId);
            }
        }

        /**
         * Add user ID to MDC for all subsequent log statements in this thread
         */
        public static void setUserId(Long userId) {
            if (userId != null) {
                MDC.put(USER_ID_MDC_KEY, userId.toString());
            }
        }

        /**
         * Clear user ID from MDC
         */
        public static void clearUserId() {
            MDC.remove(USER_ID_MDC_KEY);
        }

        /**
         * Get current correlation ID
         */
        public static String getCorrelationId() {
            return MDC.get(CORRELATION_ID_MDC_KEY);
        }

        /**
         * Get current user ID
         */
        public static String getUserId() {
            return MDC.get(USER_ID_MDC_KEY);
        }

        /**
         * Add custom context to MDC
         */
        public static void addContext(String key, String value) {
            if (key != null && value != null) {
                MDC.put(key, value);
            }
        }

        /**
         * Remove custom context from MDC
         */
        public static void removeContext(String key) {
            if (key != null) {
                MDC.remove(key);
            }
        }
    }
}
