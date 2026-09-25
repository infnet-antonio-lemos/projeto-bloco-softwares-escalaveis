package br.edu.infnet.appointment.events;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@ConditionalOnProperty(name = "petclinic.events.enabled", havingValue = "true", matchIfMissing = true)
public class RabbitTopologyConfig {

    // --- Exchanges ---------------------------------------------------------

    @Bean
    TopicExchange registryExchange() {
        return new TopicExchange(EventContract.REGISTRY_EXCHANGE, true, false);
    }

    @Bean
    TopicExchange schedulingExchange() {
        return new TopicExchange(EventContract.SCHEDULING_EXCHANGE, true, false);
    }

    // --- Filas de consumo --------------------------------------------------

    @Bean
    Queue petEventsQueue() {
        return QueueBuilder.durable(EventContract.PET_EVENTS_QUEUE).build();
    }

    @Bean
    Queue ownerEventsQueue() {
        return QueueBuilder.durable(EventContract.OWNER_EVENTS_QUEUE).build();
    }

    // --- Bindings ----------------------------------------------------------

    @Bean
    Binding petEventsBinding() {
        return BindingBuilder.bind(petEventsQueue())
                .to(registryExchange())
                .with(EventContract.PET_ROUTING_PATTERN);
    }

    @Bean
    Binding ownerEventsBinding() {
        return BindingBuilder.bind(ownerEventsQueue())
                .to(registryExchange())
                .with(EventContract.OWNER_ROUTING_PATTERN);
    }

    // --- Serialização ------------------------------------------------------

    @Bean
    MessageConverter eventMessageConverter(JsonMapper jsonMapper) {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter(jsonMapper);
        converter.setTypePrecedence(JacksonJavaTypeMapper.TypePrecedence.INFERRED);
        converter.setAlwaysConvertToInferredType(true);
        return converter;
    }
}
