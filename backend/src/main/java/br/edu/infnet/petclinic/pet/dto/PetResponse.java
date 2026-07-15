package br.edu.infnet.petclinic.pet.dto;

import br.edu.infnet.petclinic.pet.Pet;
import br.edu.infnet.petclinic.pet.Species;

import java.time.Instant;
import java.time.LocalDate;

public record PetResponse(
    Long id,
    String name,
    Species species,
    String breed,
    LocalDate birthDate,
    Long ownerId,
    String ownerName,
    Instant createdAt,
    Instant updatedAt
) {
    public static PetResponse from(Pet pet) {
        return new PetResponse(
            pet.getId(),
            pet.getName(),
            pet.getSpecies(),
            pet.getBreed(),
            pet.getBirthDate(),
            pet.getOwner() == null ? null : pet.getOwner().getId(),
            pet.getOwner() == null ? null : pet.getOwner().getName(),
            pet.getCreatedAt(),
            pet.getUpdatedAt()
        );
    }
}
