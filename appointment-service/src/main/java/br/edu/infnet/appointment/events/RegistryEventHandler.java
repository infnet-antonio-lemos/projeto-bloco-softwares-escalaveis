package br.edu.infnet.appointment.events;

import br.edu.infnet.appointment.appointment.Appointment;
import br.edu.infnet.appointment.appointment.AppointmentRepository;
import br.edu.infnet.appointment.appointment.AppointmentStatus;
import br.edu.infnet.appointment.events.dto.AppointmentSnapshot;
import br.edu.infnet.appointment.events.dto.OwnerEventMessage;
import br.edu.infnet.appointment.events.dto.OwnerSnapshot;
import br.edu.infnet.appointment.events.dto.PetEventMessage;
import br.edu.infnet.appointment.events.dto.PetSnapshot;
import br.edu.infnet.appointment.projection.PetView;
import br.edu.infnet.appointment.projection.PetViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistryEventHandler {

    private final PetViewRepository petViews;
    private final AppointmentRepository appointments;
    private final DomainEventPublisher events;

    @Transactional
    public void onPetEvent(PetEventMessage message) {
        PetSnapshot pet = message.payload();

        switch (message.eventType()) {
            case EventContract.PET_CREATED, EventContract.PET_UPDATED -> {
                upsert(pet, message.occurredAt());
                refreshScheduledAppointments(pet);
            }
            case EventContract.PET_DELETED -> {
                forget(pet.id());
                cancelScheduled(
                        appointments.findByPetIdAndStatus(pet.id(), AppointmentStatus.SCHEDULED),
                        AppointmentSnapshot.TRIGGER_PET_DELETED);
            }
            // Um tipo desconhecido não é erro
            default -> log.debug("Evento de pet ignorado (tipo não tratado): {}", message.eventType());
        }
    }

    @Transactional
    public void onOwnerEvent(OwnerEventMessage message) {
        OwnerSnapshot owner = message.payload();

        switch (message.eventType()) {
            case EventContract.OWNER_UPDATED -> renameOwner(owner, message.occurredAt());
            case EventContract.OWNER_DELETED -> {
                // O monolito apaga os pets em cascata sem emitir pet.deleted para cada um;
                // a lista do payload é a única forma de saber quais projeções invalidar.
                List<Long> petIds = owner.petIds() == null ? List.of() : owner.petIds();
                petIds.forEach(this::forget);
                cancelScheduled(
                        appointments.findByOwnerIdAndStatus(owner.id(), AppointmentStatus.SCHEDULED),
                        AppointmentSnapshot.TRIGGER_OWNER_DELETED);
            }
            default -> log.debug("Evento de tutor ignorado (tipo não tratado): {}", message.eventType());
        }
    }

    // --- Projeção ----------------------------------------------------------

    private void upsert(PetSnapshot pet, Instant occurredAt) {
        PetView view = petViews.findById(pet.id()).orElseGet(() -> PetView.builder().id(pet.id()).build());
        if (isStale(view, occurredAt)) {
            return;
        }
        view.setName(pet.name());
        view.setSpecies(pet.species());
        view.setBreed(pet.breed());
        view.setOwnerId(pet.ownerId());
        view.setOwnerName(pet.ownerName());
        view.setLastEventAt(occurredAt);
        view.setUpdatedAt(Instant.now());
        petViews.save(view);
    }

    private void forget(Long petId) {
        petViews.deleteById(petId);
    }

    private void renameOwner(OwnerSnapshot owner, Instant occurredAt) {
        petViews.findByOwnerId(owner.id()).forEach(view -> {
            if (isStale(view, occurredAt)) {
                return;
            }
            view.setOwnerName(owner.name());
            view.setLastEventAt(occurredAt);
            view.setUpdatedAt(Instant.now());
            petViews.save(view);
        });

        appointments.findByOwnerIdAndStatus(owner.id(), AppointmentStatus.SCHEDULED).forEach(appointment -> {
            if (!Objects.equals(appointment.getOwnerName(), owner.name())) {
                appointment.setOwnerName(owner.name());
                appointments.save(appointment);
            }
        });
    }

    private boolean isStale(PetView view, Instant occurredAt) {
        return view.getLastEventAt() != null
                && occurredAt != null
                && occurredAt.isBefore(view.getLastEventAt());
    }

    // --- Reações sobre as consultas ----------------------------------------

    private void refreshScheduledAppointments(PetSnapshot pet) {
        appointments.findByPetIdAndStatus(pet.id(), AppointmentStatus.SCHEDULED).forEach(appointment -> {
            boolean changed = !Objects.equals(appointment.getPetName(), pet.name())
                    || !Objects.equals(appointment.getOwnerId(), pet.ownerId())
                    || !Objects.equals(appointment.getOwnerName(), pet.ownerName());
            if (changed) {
                appointment.setPetName(pet.name());
                appointment.setOwnerId(pet.ownerId());
                appointment.setOwnerName(pet.ownerName());
                appointments.save(appointment);
                log.info("Consulta {} sincronizada com o cadastro do pet {}", appointment.getId(), pet.id());
            }
        });
    }

    private void cancelScheduled(List<Appointment> scheduled, String trigger) {
        scheduled.forEach(appointment -> {
            appointment.setStatus(AppointmentStatus.CANCELLED);
            appointments.save(appointment);
            events.publish(EventContract.APPOINTMENT_CANCELLED, "appointment", appointment.getId(),
                    AppointmentSnapshot.of(appointment, trigger));
            log.info("Consulta {} cancelada em cascata ({})", appointment.getId(), trigger);
        });
    }
}
