package br.edu.infnet.appointment.appointment;

import br.edu.infnet.appointment.appointment.dto.AppointmentRequest;
import br.edu.infnet.appointment.appointment.dto.AppointmentResponse;
import br.edu.infnet.appointment.events.DomainEventPublisher;
import br.edu.infnet.appointment.events.EventContract;
import br.edu.infnet.appointment.events.dto.AppointmentSnapshot;
import br.edu.infnet.appointment.projection.PetDirectory;
import br.edu.infnet.appointment.projection.ResolvedPet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional
public class AppointmentService {

    private final AppointmentRepository repository;
    private final PetDirectory petDirectory;
    private final DomainEventPublisher events;

    @Transactional(readOnly = true)
    public List<AppointmentResponse> findAll(Long petId, Long ownerId, AppointmentStatus status) {
        if (petId != null) {
            return toResponses(repository.findByPetIdOrderByScheduledAtDesc(petId));
        }
        if (ownerId != null) {
            return toResponses(repository.findByOwnerIdOrderByScheduledAtDesc(ownerId));
        }
        if (status != null) {
            return toResponses(repository.findByStatusOrderByScheduledAtDesc(status));
        }
        return toResponses(repository.findAllByOrderByScheduledAtDesc());
    }

    @Transactional(readOnly = true)
    public AppointmentResponse findById(Long id) {
        return repository.findById(id)
                .map(AppointmentResponse::from)
                .orElseThrow(() -> new NoSuchElementException("Appointment not found: " + id));
    }

    public AppointmentResponse create(AppointmentRequest request) {
        ResolvedPet pet = petDirectory.resolve(request.petId());
        checkVetAvailability(request);

        Appointment appointment = repository.save(Appointment.builder()
                .petId(pet.id())
                .ownerId(pet.ownerId())
                .petName(pet.name())
                .ownerName(pet.ownerName())
                .scheduledAt(request.scheduledAt())
                .veterinarian(request.veterinarian())
                .reason(request.reason())
                .notes(request.notes())
                .status(AppointmentStatus.SCHEDULED)
                .build());

        events.publish(EventContract.APPOINTMENT_SCHEDULED, "appointment", appointment.getId(),
                AppointmentSnapshot.of(appointment, AppointmentSnapshot.TRIGGER_USER));
        return AppointmentResponse.from(appointment);
    }

    public AppointmentResponse update(Long id, AppointmentRequest request) {
        Appointment appointment = require(id);
        ResolvedPet pet = petDirectory.resolve(request.petId());

        LocalDateTime previousScheduledAt = appointment.getScheduledAt();
        boolean rescheduled = !appointment.getVeterinarian().equals(request.veterinarian())
                || !appointment.getScheduledAt().equals(request.scheduledAt());
        // Só revalida a agenda quando horário ou veterinário mudaram — caso contrário a
        // própria consulta sendo editada apareceria como conflito consigo mesma.
        if (rescheduled) {
            checkVetAvailability(request);
        }

        appointment.setPetId(pet.id());
        appointment.setOwnerId(pet.ownerId());
        appointment.setPetName(pet.name());
        appointment.setOwnerName(pet.ownerName());
        appointment.setScheduledAt(request.scheduledAt());
        appointment.setVeterinarian(request.veterinarian());
        appointment.setReason(request.reason());
        appointment.setNotes(request.notes());
        repository.save(appointment);

        // Remarcação e edição de detalhes têm routing keys distintas: o tutor precisa ser
        // avisado de uma mudança de horário, não de uma correção de observação.
        String eventType = rescheduled
                ? EventContract.APPOINTMENT_RESCHEDULED
                : EventContract.APPOINTMENT_UPDATED;
        events.publish(eventType, "appointment", appointment.getId(),
                AppointmentSnapshot.of(appointment, AppointmentSnapshot.TRIGGER_USER,
                        rescheduled ? previousScheduledAt : null));
        return AppointmentResponse.from(appointment);
    }

    public AppointmentResponse updateStatus(Long id, AppointmentStatus status) {
        Appointment appointment = require(id);
        appointment.setStatus(status);
        repository.save(appointment);

        events.publish(eventTypeFor(status), "appointment", appointment.getId(),
                AppointmentSnapshot.of(appointment, AppointmentSnapshot.TRIGGER_USER));
        return AppointmentResponse.from(appointment);
    }

    public void delete(Long id) {
        Appointment appointment = require(id);
        AppointmentSnapshot snapshot =
                AppointmentSnapshot.of(appointment, AppointmentSnapshot.TRIGGER_USER);
        repository.delete(appointment);
        events.publish(EventContract.APPOINTMENT_DELETED, "appointment", id, snapshot);
    }

    /**
     * Cada estado final tem seu próprio evento, em vez de um genérico
     * {@code appointment.status-changed}: assim o consumidor filtra pelo binding do
     * broker e não precisa receber tudo para descartar em código o que não lhe serve.
     */
    private String eventTypeFor(AppointmentStatus status) {
        return switch (status) {
            case SCHEDULED -> EventContract.APPOINTMENT_SCHEDULED;
            case COMPLETED -> EventContract.APPOINTMENT_COMPLETED;
            case CANCELLED -> EventContract.APPOINTMENT_CANCELLED;
            case NO_SHOW -> EventContract.APPOINTMENT_NO_SHOW;
        };
    }

    private Appointment require(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Appointment not found: " + id));
    }

    /** Um veterinário não pode ter duas consultas ativas no mesmo horário. */
    private void checkVetAvailability(AppointmentRequest request) {
        boolean taken = repository.existsByVeterinarianAndScheduledAtAndStatus(
                request.veterinarian(), request.scheduledAt(), AppointmentStatus.SCHEDULED);
        if (taken) {
            throw new IllegalStateException(
                    "O veterinário %s já possui consulta agendada em %s."
                            .formatted(request.veterinarian(), request.scheduledAt()));
        }
    }

    private List<AppointmentResponse> toResponses(List<Appointment> appointments) {
        return appointments.stream().map(AppointmentResponse::from).toList();
    }
}
