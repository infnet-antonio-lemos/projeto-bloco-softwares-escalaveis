package br.edu.infnet.appointment.events.dto;

import br.edu.infnet.appointment.appointment.Appointment;

import java.time.LocalDateTime;

public record AppointmentSnapshot(
        Long id,
        Long petId,
        String petName,
        Long ownerId,
        String ownerName,
        LocalDateTime scheduledAt,
        LocalDateTime previousScheduledAt,
        String veterinarian,
        String reason,
        String status,
        String trigger
) {

    /** Origem da mudança: ação direta do usuário sobre a consulta. */
    public static final String TRIGGER_USER = "USER";
    /** Origem da mudança: reação em cascata a um {@code pet.deleted}. */
    public static final String TRIGGER_PET_DELETED = "PET_DELETED";
    /** Origem da mudança: reação em cascata a um {@code owner.deleted}. */
    public static final String TRIGGER_OWNER_DELETED = "OWNER_DELETED";

    public static AppointmentSnapshot of(Appointment appointment, String trigger) {
        return of(appointment, trigger, null);
    }

    public static AppointmentSnapshot of(
            Appointment appointment, String trigger, LocalDateTime previousScheduledAt) {
        return new AppointmentSnapshot(
                appointment.getId(),
                appointment.getPetId(),
                appointment.getPetName(),
                appointment.getOwnerId(),
                appointment.getOwnerName(),
                appointment.getScheduledAt(),
                previousScheduledAt,
                appointment.getVeterinarian(),
                appointment.getReason(),
                appointment.getStatus() == null ? null : appointment.getStatus().name(),
                trigger);
    }
}
