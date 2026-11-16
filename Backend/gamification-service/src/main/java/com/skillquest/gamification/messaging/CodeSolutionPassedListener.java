package com.skillquest.gamification.messaging;

import com.skillquest.gamification.config.GamificationProperties;
import com.skillquest.gamification.service.GamificationService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Consomme {@code CodeSolutionPassedEvent} : +50 XP et recalcul du niveau. */
@Component
public class CodeSolutionPassedListener {

    private static final Logger log = LoggerFactory.getLogger(CodeSolutionPassedListener.class);

    private final GamificationService gamificationService;
    private final GamificationProperties props;

    public CodeSolutionPassedListener(GamificationService gamificationService, GamificationProperties props) {
        this.gamificationService = gamificationService;
        this.props = props;
    }

    @RabbitListener(queues = "${gamification.messaging.code-solution-queue}")
    public void onCodeSolutionPassed(CodeSolutionPassedEvent event) {
        if (event == null || event.userId() == null) {
            throw new AmqpRejectAndDontRequeueException("CodeSolutionPassedEvent without userId");
        }
        boolean applied = gamificationService.addXPForEvent(
                event.eventId(), event.userId(), props.xp().codeSolutionPassed());
        log.info("CodeSolutionPassedEvent user={} challenge={} applied={}",
                event.userId(), event.challengeId(), applied);
    }
}
