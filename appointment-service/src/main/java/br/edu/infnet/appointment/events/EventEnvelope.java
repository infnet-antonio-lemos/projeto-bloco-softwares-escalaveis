package br.edu.infnet.appointment.events;

import java.time.Instant;
import java.util.UUID;

/** Envelope de publicação — mesma forma do envelope recebido, ver {@link EventContract}. */
public record EventEnvelope<T>(
        String eventId,
        String eventType,
        Instant occurredAt,
        String aggregateType,
        String aggregateId,
        int version,
        String correlationId,
        T payload
) {

    public static <T> EventEnvelope<T> of(
            String eventType, String aggregateType, String aggregateId, String correlationId, T payload) {
        return new EventEnvelope<>(
                UUID.randomUUID().toString(),
                eventType,
                Instant.now(),
                aggregateType,
                aggregateId,
                EventContract.SCHEMA_VERSION,
                correlationId,
                payload);
    }
}
