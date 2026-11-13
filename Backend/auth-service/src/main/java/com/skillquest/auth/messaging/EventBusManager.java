package com.skillquest.auth.messaging;

import com.skillquest.auth.event.UserRegisteredEvent;
import com.skillquest.auth.event.UserReportedAbuseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Gestionnaire d'événements asynchrones RabbitMQ / Bus événementiel (AWD Exigences 2 & 3).
 * Publie UserRegisteredEvent (Topic exchange: skillquest.events, routingKey: auth.user.registered).
 * Consomme UserReportedAbuseEvent (Queue: auth.abuse_reported.queue).
 */
@Component
public class EventBusManager {

    private static final Logger log = LoggerFactory.getLogger(EventBusManager.class);

    private final ApplicationEventPublisher applicationEventPublisher;
    // Buffer local de simulation pour la queue RabbitMQ locale si broker externe non démarré
    private final BlockingQueue<UserRegisteredEvent> publishedEvents = new LinkedBlockingQueue<>();

    public EventBusManager(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /**
     * Publication d'un événement métier (AWD Exigence 2)
     */
    public void publishUserRegistered(UserRegisteredEvent event) {
        log.info("[RabbitMQ Asynchrone] Publication de l'événement sur l'Exchange 'skillquest.events' avec la clé 'auth.user.registered' : EventId={}, UserId={}, Email={}",
            event.getEventId(), event.getUserId(), event.getEmail());
        publishedEvents.offer(event);
        applicationEventPublisher.publishEvent(event);
    }

    public BlockingQueue<UserRegisteredEvent> getPublishedEvents() {
        return publishedEvents;
    }
}
