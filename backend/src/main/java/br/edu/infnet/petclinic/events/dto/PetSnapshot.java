package br.edu.infnet.petclinic.events.dto;

public record PetSnapshot(
        Long id,
        String name,
        String species,
        String breed,
        Long ownerId,
        String ownerName
) {}
