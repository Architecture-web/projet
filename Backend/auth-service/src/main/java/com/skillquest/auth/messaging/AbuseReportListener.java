package com.skillquest.auth.messaging;

import com.skillquest.auth.event.UserReportedAbuseEvent;
import com.skillquest.auth.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listener pour la queue RabbitMQ 'auth.abuse_reported.queue'.
 * Consomme l'événement UserReportedAbuseEvent (AWD Exigence 3).
 */
@Component
public class AbuseReportListener {

    private static final Logger log = LoggerFactory.getLogger(AbuseReportListener.class);

    private final AuthService authService;

    public AbuseReportListener(AuthService authService) {
        this.authService = authService;
    }

    @EventListener
    public void onAbuseReported(UserReportedAbuseEvent event) {
        log.info("[AbuseReportListener] Réception de l'événement UserReportedAbuseEvent depuis la file RabbitMQ 'auth.abuse_reported.queue'");
        authService.handleUserReportedAbuse(event);
    }
}
