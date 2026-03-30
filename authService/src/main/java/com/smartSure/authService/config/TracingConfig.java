package com.smartSure.authService.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

/**
 * Distributed Logging Configuration for AuthService.
 *
 * Tracing sampling is configured via application.properties:
 *   management.tracing.sampling.probability=1.0
 *
 * This class only sets up HTTP request logging.
 *
 * @author SmartSure Development Team
 * @version 2.1
 */
@Slf4j
@Configuration
public class TracingConfig {

    /**
     * Logs incoming HTTP requests for debugging and monitoring.
     * Payload logging is limited to 500 characters to avoid log bloat.
     *
     * @return configured CommonsRequestLoggingFilter
     */
    @Bean
    public CommonsRequestLoggingFilter requestLoggingFilter() {
        log.info("Configuring HTTP request logging filter");
        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
        filter.setIncludeClientInfo(true);
        filter.setIncludeQueryString(true);
        filter.setIncludePayload(true);
        filter.setIncludeHeaders(false);
        filter.setMaxPayloadLength(500);
        filter.setBeforeMessagePrefix("REQUEST: ");
        filter.setAfterMessagePrefix("COMPLETED: ");
        return filter;
    }
}
