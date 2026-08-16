package br.edu.infnet.appointment.appointment.dto;

import br.edu.infnet.appointment.appointment.Appointment;
import br.edu.infnet.appointment.appointment.AppointmentStatus;

import java.time.Instant;
import java.time.LocalDateTime;

public record AppointmentResponse(
    Long id,
    Long petId,
    String petName,
    Long ownerId,
    String ownerName,
    LocalDateTime scheduledAt,
    String veterinarian,
    String reason,
    String notes,
    AppointmentStatus status,
    Instant createdAt,
    Instant updatedAt
) {
    public static AppointmentResponse from(Appointment appointment) {
        return new AppointmentResponse(
            appointment.getId(),
            appointment.getPetId(),
            appointment.getPetName(),
            appointment.getOwnerId(),
            appointment.getOwnerName(),
            appointment.getScheduledAt(),
            appointment.getVeterinarian(),
            appointment.getReason(),
            appointment.getNotes(),
            appointment.getStatus(),
            appointment.getCreatedAt(),
            appointment.getUpdatedAt()
        );
    }
}
