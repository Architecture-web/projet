package com.skillquest.gamification.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologie RabbitMQ : exchange topic partagé {@code skillquest.events}, deux queues consommées
 * par ce service et une dead-letter queue pour les messages en échec définitif.
 */
@Configuration
public class RabbitConfig {

    private static final String DLX_ARGUMENT = "x-dead-letter-exchange";

    @Bean
    TopicExchange eventsExchange(GamificationProperties props) {
        return new TopicExchange(props.messaging().exchange(), true, false);
    }

    @Bean
    FanoutExchange deadLetterExchange(GamificationProperties props) {
        return new FanoutExchange(props.messaging().deadLetterExchange(), true, false);
    }

    @Bean
    Queue deadLetterQueue(GamificationProperties props) {
        return QueueBuilder.durable(props.messaging().deadLetterQueue()).build();
    }

    @Bean
    Binding deadLetterBinding(Queue deadLetterQueue, FanoutExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange);
    }

    @Bean
    Queue codeSolutionQueue(GamificationProperties props) {
        return QueueBuilder.durable(props.messaging().codeSolutionQueue())
                .withArgument(DLX_ARGUMENT, props.messaging().deadLetterExchange())
                .build();
    }

    @Bean
    Binding codeSolutionBinding(Queue codeSolutionQueue, TopicExchange eventsExchange, GamificationProperties props) {
        return BindingBuilder.bind(codeSolutionQueue).to(eventsExchange)
                .with(props.messaging().codeSolutionRoutingKey());
    }

    @Bean
    Queue courseCompletedQueue(GamificationProperties props) {
        return QueueBuilder.durable(props.messaging().courseCompletedQueue())
                .withArgument(DLX_ARGUMENT, props.messaging().deadLetterExchange())
                .build();
    }

    @Bean
    Binding courseCompletedBinding(Queue courseCompletedQueue, TopicExchange eventsExchange,
                                   GamificationProperties props) {
        return BindingBuilder.bind(courseCompletedQueue).to(eventsExchange)
                .with(props.messaging().courseCompletedRoutingKey());
    }

    /**
     * Convertisseur JSON. Le type cible est déduit du paramètre de la méthode {@code @RabbitListener}
     * (et non de l'en-tête {@code __TypeId__}), car les producteurs sont d'autres microservices
     * dont les classes Java n'existent pas ici.
     */
    @Bean
    MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}
