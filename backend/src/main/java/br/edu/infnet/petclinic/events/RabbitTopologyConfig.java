package br.edu.infnet.petclinic.events;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@ConditionalOnProperty(name = "petclinic.events.enabled", havingValue = "true", matchIfMissing = true)
public class RabbitTopologyConfig {

    @Bean
    TopicExchange registryExchange() {
        return new TopicExchange(EventContract.REGISTRY_EXCHANGE, true, false);
    }

    @Bean
    MessageConverter eventMessageConverter(JsonMapper jsonMapper) {
        return new JacksonJsonMessageConverter(jsonMapper);
    }
}
