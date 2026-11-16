package com.skillquest.gamification.messaging;

import com.skillquest.gamification.config.GamificationProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publie {@link BadgeUnlockedEvent} vers RabbitMQ <b>après le commit</b> de la transaction
 * qui a débloqué le badge : aucun événement n'est émis si la transaction est annulée.
 */
@Component
public class BadgeEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(BadgeEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final GamificationProperties props;

    public BadgeEventPublisher(RabbitTemplate rabbitTemplate, GamificationProperties props) {
        this.rabbitTemplate = rabbitTemplate;
        this.props = props;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void publish(BadgeUnlockedEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    props.messaging().exchange(),
                    props.messaging().badgeUnlockedRoutingKey(),
                    event);
            log.info("Published BadgeUnlockedEvent user={} badge={}", event.userId(), event.badgeCode());
        } catch (AmqpException ex) {
            // Le badge est déjà enregistré en base : on journalise sans faire échouer la requête métier.
            log.error("Could not publish BadgeUnlockedEvent user={} badge={}", event.userId(), event.badgeCode(), ex);
        }
    }
}
