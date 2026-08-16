package br.edu.infnet.petclinic.pet.dto;

import br.edu.infnet.petclinic.pet.Species;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

// As mensagens são frases completas porque o GlobalExceptionHandler devolve
// apenas o defaultMessage — sem o nome do campo — direto para a tela.
public record PetRequest(
    @NotBlank(message = "Informe o nome do pet.")
    String name,

    @NotNull(message = "Selecione a espécie do pet.")
    Species species,

    String breed,
    LocalDate birthDate,

    @NotNull(message = "Selecione o tutor do pet.")
    Long ownerId
) {}
