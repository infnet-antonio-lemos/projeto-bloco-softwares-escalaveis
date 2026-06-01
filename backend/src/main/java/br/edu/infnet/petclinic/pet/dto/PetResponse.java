package br.edu.infnet.petclinic.pet.dto;

import br.edu.infnet.petclinic.pet.Pet;
import br.edu.infnet.petclinic.pet.Species;

import java.time.LocalDate;

public record PetResponse(
    Long id,
    String name,
    Species species,
    String breed,
    LocalDate birthDate,
    Long ownerId,
    String ownerName
) {
    public static PetResponse from(Pet pet) {
        return new PetResponse(
            pet.getId(),
            pet.getName(),
            pet.getSpecies(),
            pet.getBreed(),
            pet.getBirthDate(),
            pet.getOwner().getId(),
            pet.getOwner().getName()
        );
    }
}
