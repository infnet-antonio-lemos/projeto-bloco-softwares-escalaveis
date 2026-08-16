package br.edu.infnet.appointment.appointment;

/**
 * Ciclo de vida de uma consulta. Persistido e serializado como string.
 */
public enum AppointmentStatus {
    /** Agendada e ainda não realizada. */
    SCHEDULED,
    /** Consulta realizada. */
    COMPLETED,
    /** Cancelada pelo tutor ou pela clínica. */
    CANCELLED,
    /** O tutor não compareceu. */
    NO_SHOW
}
