package br.edu.infnet.appointment.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Projeção dos campos do {@code PetResponse} do monolito que interessam ao agendamento.
 *
 * <p>{@code @JsonIgnoreProperties(ignoreUnknown = true)} é deliberado: o contrato do
 * monolito pode ganhar campos novos (breed, birthDate, timestamps…) sem quebrar este
 * serviço — acoplamento mínimo entre os contextos.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PetSummary(
    Long id,
    String name,
    Long ownerId,
    String ownerName
) {}
