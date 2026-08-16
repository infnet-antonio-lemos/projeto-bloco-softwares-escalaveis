package br.edu.infnet.appointment.appointment;

import br.edu.infnet.appointment.appointment.dto.AppointmentRequest;
import br.edu.infnet.appointment.appointment.dto.AppointmentResponse;
import br.edu.infnet.appointment.client.PetClient;
import br.edu.infnet.appointment.client.dto.PetSummary;
import br.edu.infnet.appointment.config.PetRegistryUnavailableException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional
public class AppointmentService {

    private final AppointmentRepository repository;
    private final PetClient petClient;

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
        PetSummary pet = fetchPet(request.petId());
        checkVetAvailability(request);

        Appointment appointment = Appointment.builder()
                .petId(pet.id())
                .ownerId(pet.ownerId())
                .petName(pet.name())
                .ownerName(pet.ownerName())
                .scheduledAt(request.scheduledAt())
                .veterinarian(request.veterinarian())
                .reason(request.reason())
                .notes(request.notes())
                .status(AppointmentStatus.SCHEDULED)
                .build();
        return AppointmentResponse.from(repository.save(appointment));
    }

    public AppointmentResponse update(Long id, AppointmentRequest request) {
        Appointment appointment = require(id);
        // Reconsulta o monolito: o pet pode ter mudado de tutor ou de nome desde o agendamento
        PetSummary pet = fetchPet(request.petId());
        // Só revalida a agenda se o horário ou o veterinário mudaram — caso contrário
        // a própria consulta sendo editada apareceria como conflito consigo mesma.
        if (!appointment.getVeterinarian().equals(request.veterinarian())
                || !appointment.getScheduledAt().equals(request.scheduledAt())) {
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
        return AppointmentResponse.from(repository.save(appointment));
    }

    public AppointmentResponse updateStatus(Long id, AppointmentStatus status) {
        Appointment appointment = require(id);
        appointment.setStatus(status);
        return AppointmentResponse.from(repository.save(appointment));
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NoSuchElementException("Appointment not found: " + id);
        }
        repository.deleteById(id);
    }

    /**
     * Chamada síncrona ao contexto Patient Registry.
     *
     * <p>A distinção entre os dois catch é o ponto central: 404 do monolito significa
     * que o pet não existe (erro do cliente); qualquer outra falha significa que o
     * serviço dependente está indisponível (erro de infraestrutura). Traduzir os dois
     * para o mesmo status esconderia uma queda do monolito.
     */
    private PetSummary fetchPet(Long petId) {
        try {
            return petClient.getPet(petId);
        } catch (FeignException.NotFound ex) {
            throw new NoSuchElementException("Pet not found: " + petId);
        } catch (FeignException ex) {
            throw new PetRegistryUnavailableException(
                    "Cadastro de pets indisponível no momento. Tente novamente.", ex);
        }
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
