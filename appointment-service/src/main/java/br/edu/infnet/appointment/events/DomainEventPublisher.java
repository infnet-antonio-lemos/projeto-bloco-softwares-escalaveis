package br.edu.infnet.appointment.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DomainEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${petclinic.events.enabled:true}")
    private boolean enabled;

    public void publish(String eventType, String aggregateType, Object aggregateId, Object payload) {
        EventEnvelope<Object> envelope = EventEnvelope.of(
                eventType, aggregateType, String.valueOf(aggregateId), MDC.get(CorrelationId.MDC_KEY), payload);

        if (!enabled) {
            log.debug("Mensageria desligada — evento {} não publicado", eventType);
            return;
        }

        rabbitTemplate.convertAndSend(EventContract.SCHEDULING_EXCHANGE, eventType, envelope, message -> {
            message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            message.getMessageProperties().setMessageId(envelope.eventId());
            message.getMessageProperties().setHeader("x-event-type", eventType);
            message.getMessageProperties().setHeader("x-aggregate-type", aggregateType);
            message.getMessageProperties().setHeader("x-aggregate-id", envelope.aggregateId());
            message.getMessageProperties().setHeader("x-schema-version", EventContract.SCHEMA_VERSION);
            return message;
        });

        log.info("Evento {} publicado (eventId={}, aggregateId={})",
                eventType, envelope.eventId(), envelope.aggregateId());
    }
}
