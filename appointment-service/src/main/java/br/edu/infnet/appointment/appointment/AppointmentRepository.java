package br.edu.infnet.appointment.appointment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositório dedicado do microsserviço — opera exclusivamente sobre {@code appointmentsdb}.
 * Todas as consultas são derived queries, no mesmo estilo do {@code PetRepository} do monolito.
 */
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findAllByOrderByScheduledAtDesc();

    List<Appointment> findByPetIdOrderByScheduledAtDesc(Long petId);

    List<Appointment> findByOwnerIdOrderByScheduledAtDesc(Long ownerId);

    List<Appointment> findByStatusOrderByScheduledAtDesc(AppointmentStatus status);

    /** Regra de agenda: um veterinário não pode ter duas consultas ativas no mesmo horário. */
    boolean existsByVeterinarianAndScheduledAtAndStatus(
            String veterinarian, LocalDateTime scheduledAt, AppointmentStatus status);
}
