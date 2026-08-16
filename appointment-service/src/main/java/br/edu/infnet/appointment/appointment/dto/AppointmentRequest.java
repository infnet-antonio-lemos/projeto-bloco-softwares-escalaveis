package br.edu.infnet.appointment.appointment.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Note que não há {@code ownerId} aqui: o tutor é resolvido a partir do pet
 * pelo cliente Feign. O cliente informa apenas o que ele sabe.
 */
public record AppointmentRequest(
    @NotNull(message = "Selecione o pet da consulta.")
    Long petId,

    // As mensagens são frases completas porque o GlobalExceptionHandler devolve
    // apenas o defaultMessage — sem o nome do campo — direto para a tela.
    @NotNull(message = "Informe a data e a hora da consulta.")
    @Future(message = "A data da consulta deve ser no futuro.")
    LocalDateTime scheduledAt,

    @NotBlank(message = "Informe o veterinário responsável.")
    String veterinarian,

    String reason,

    @Size(max = 1000, message = "As observações devem ter no máximo 1000 caracteres.")
    String notes
) {}
