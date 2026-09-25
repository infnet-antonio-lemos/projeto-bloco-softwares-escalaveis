package br.edu.infnet.appointment.events.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** Payload dos eventos {@code owner.*}. {@code petIds} cobre o delete em cascata. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OwnerSnapshot(
        Long id,
        String name,
        String email,
        String phone,
        List<Long> petIds
) {}
