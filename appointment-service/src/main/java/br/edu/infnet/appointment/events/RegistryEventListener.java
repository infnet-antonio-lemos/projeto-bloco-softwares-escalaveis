package br.edu.infnet.appointment.events;

import br.edu.infnet.appointment.events.dto.OwnerEventMessage;
import br.edu.infnet.appointment.events.dto.PetEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "petclinic.events.enabled", havingValue = "true", matchIfMissing = true)
public class RegistryEventListener {

    private final RegistryEventHandler handler;

    @RabbitListener(queues = EventContract.PET_EVENTS_QUEUE)
    public void onPetEvent(PetEventMessage message) {
        withCorrelation(message.correlationId(), () -> {
            log.info("Recebido {} (eventId={}, petId={})",
                    message.eventType(), message.eventId(), message.aggregateId());
            handler.onPetEvent(message);
        });
    }

    @RabbitListener(queues = EventContract.OWNER_EVENTS_QUEUE)
    public void onOwnerEvent(OwnerEventMessage message) {
        withCorrelation(message.correlationId(), () -> {
            log.info("Recebido {} (eventId={}, ownerId={})",
                    message.eventType(), message.eventId(), message.aggregateId());
            handler.onOwnerEvent(message);
        });
    }

    private void withCorrelation(String correlationId, Runnable action) {
        if (correlationId != null) {
            MDC.put(CorrelationId.MDC_KEY, correlationId);
        }
        try {
            action.run();
        } finally {
            MDC.remove(CorrelationId.MDC_KEY);
        }
    }
}
