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
    @NotNull Long petId,
    @NotNull @Future LocalDateTime scheduledAt,
    @NotBlank String veterinarian,
    String reason,
    @Size(max = 1000) String notes
) {}
