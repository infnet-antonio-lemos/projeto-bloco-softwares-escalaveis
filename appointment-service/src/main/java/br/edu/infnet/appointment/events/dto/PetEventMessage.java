package br.edu.infnet.appointment.events.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PetEventMessage(
        String eventId,
        String eventType,
        Instant occurredAt,
        String aggregateId,
        int version,
        String correlationId,
        PetSnapshot payload
) {}
