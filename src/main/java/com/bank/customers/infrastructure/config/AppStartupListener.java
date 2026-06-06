package com.bank.customers.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class AppStartupListener {

    private static final Logger log = LoggerFactory.getLogger(AppStartupListener.class);

    private final Environment environment;

    @Value("${app.environment}")
    private String appEnvironment;

    @Value("${app.message}")
    private String appMessage;

    @Value("${server.port}")
    private String serverPort;

    @Value("${spring.application.name}")
    private String appName;

    public AppStartupListener(Environment environment) {
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("=======================================================");
        log.info("  Application : {}", appName);
        log.info("  Environment : {}", appEnvironment);
        log.info("  Port        : {}", serverPort);
        log.info("  Message     : {}", appMessage);
        log.info("  Profiles    : {}", String.join(", ", environment.getActiveProfiles()));
        log.info("  Swagger UI  : http://localhost:{}/swagger-ui.html", serverPort);
        log.info("  Health      : http://localhost:{}/actuator/health", serverPort);
        log.info("=======================================================");
    }
}
