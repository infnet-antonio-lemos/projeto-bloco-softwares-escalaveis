package br.edu.infnet.appointment.appointment.dto;

import br.edu.infnet.appointment.appointment.AppointmentStatus;
import jakarta.validation.constraints.NotNull;

/** Corpo do PATCH que move a consulta no seu ciclo de vida. */
public record AppointmentStatusRequest(
    @NotNull AppointmentStatus status
) {}
