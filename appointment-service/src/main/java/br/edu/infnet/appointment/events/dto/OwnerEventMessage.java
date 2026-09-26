package br.edu.infnet.appointment.events.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

/** Envelope tipado dos eventos {@code owner.*}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OwnerEventMessage(
        String eventId,
        String eventType,
        Instant occurredAt,
        String aggregateId,
        int version,
        String correlationId,
        OwnerSnapshot payload
) {}
