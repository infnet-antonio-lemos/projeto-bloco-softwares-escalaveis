package br.edu.infnet.petclinic.pet.dto;

import br.edu.infnet.petclinic.pet.Species;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record PetRequest(
    @NotBlank String name,
    @NotNull Species species,
    String breed,
    LocalDate birthDate,
    @NotNull Long ownerId
) {}
