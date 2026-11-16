package com.skillquest.gamification.support;

import com.skillquest.gamification.client.CourseClient;
import com.skillquest.gamification.repository.CertificateRepository;
import com.skillquest.gamification.repository.ProcessedEventRepository;
import com.skillquest.gamification.repository.UserBadgeRepository;
import com.skillquest.gamification.repository.UserProfileRepository;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Contexte Spring complet sur H2 ; RabbitMQ et course-service sont remplacés par des mocks. */
@SpringBootTest
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    @MockitoBean
    protected RabbitTemplate rabbitTemplate;

    @MockitoBean
    protected CourseClient courseClient;

    @Autowired
    private UserBadgeRepository userBadgeRepository;
    @Autowired
    private CertificateRepository certificateRepository;
    @Autowired
    private ProcessedEventRepository processedEventRepository;
    @Autowired
    private UserProfileRepository userProfileRepository;

    @BeforeEach
    void cleanDatabase() {
        userBadgeRepository.deleteAll();
        certificateRepository.deleteAll();
        processedEventRepository.deleteAll();
        userProfileRepository.deleteAll();
    }
}
