package br.edu.infnet.appointment.events.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PetSnapshot(
        Long id,
        String name,
        String species,
        String breed,
        Long ownerId,
        String ownerName
) {}
