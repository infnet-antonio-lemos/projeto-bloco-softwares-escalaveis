package br.edu.infnet.appointment.appointment;

import br.edu.infnet.appointment.appointment.dto.AppointmentRequest;
import br.edu.infnet.appointment.appointment.dto.AppointmentResponse;
import br.edu.infnet.appointment.client.PetClient;
import br.edu.infnet.appointment.client.dto.PetSummary;
import br.edu.infnet.appointment.config.PetRegistryUnavailableException;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Regras de negócio do agendamento, com o contexto Patient Registry substituído por
 * um mock do {@link PetClient} — a suíte não depende do monolito nem da rede.
 * O seed de data.sql é desligado para asserções determinísticas.
 */
@SpringBootTest(properties = "server.port=0")
@TestPropertySource(properties = "spring.sql.init.mode=never")
class AppointmentServiceTest {

    private static final PetSummary REX = new PetSummary(1L, "Rex", 7L, "Alice Souza");

    @Autowired
    private AppointmentService service;

    @Autowired
    private AppointmentRepository repository;

    @MockitoBean
    private PetClient petClient;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    void createResolvesOwnerAndNamesFromTheRemoteService() {
        when(petClient.getPet(1L)).thenReturn(REX);

        AppointmentResponse created = service.create(request(1L, futureSlot(), "Dra. Helena"));

        // Nada disso veio do request: o ownerId e os nomes foram obtidos via Feign
        assertThat(created.ownerId()).isEqualTo(7L);
        assertThat(created.petName()).isEqualTo("Rex");
        assertThat(created.ownerName()).isEqualTo("Alice Souza");
        assertThat(created.status()).isEqualTo(AppointmentStatus.SCHEDULED);
        assertThat(created.id()).isNotNull();
    }

    @Test
    void createFailsWithNotFoundWhenRemoteServiceDoesNotKnowThePet() {
        when(petClient.getPet(any())).thenThrow(feignError(404));

        assertThatThrownBy(() -> service.create(request(999L, futureSlot(), "Dra. Helena")))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Pet not found: 999");

        assertThat(repository.count()).isZero();
    }

    @Test
    void createFailsWithUnavailableWhenRemoteServiceIsDown() {
        when(petClient.getPet(any())).thenThrow(feignError(500));

        // Serviço fora do ar não pode ser reportado como "pet inexistente"
        assertThatThrownBy(() -> service.create(request(1L, futureSlot(), "Dra. Helena")))
                .isInstanceOf(PetRegistryUnavailableException.class);
    }

    @Test
    void createRejectsDoubleBookingOfTheSameVet() {
        when(petClient.getPet(1L)).thenReturn(REX);
        LocalDateTime slot = futureSlot();
        service.create(request(1L, slot, "Dra. Helena"));

        assertThatThrownBy(() -> service.create(request(1L, slot, "Dra. Helena")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Dra. Helena");

        // Outro veterinário no mesmo horário é permitido
        assertThat(service.create(request(1L, slot, "Dr. Marcos"))).isNotNull();
    }

    @Test
    void updateKeepingTheSameSlotDoesNotConflictWithItself() {
        when(petClient.getPet(1L)).thenReturn(REX);
        LocalDateTime slot = futureSlot();
        AppointmentResponse created = service.create(request(1L, slot, "Dra. Helena"));

        AppointmentResponse updated = service.update(created.id(),
                new AppointmentRequest(1L, slot, "Dra. Helena", "Novo motivo", "Nova observação"));

        assertThat(updated.reason()).isEqualTo("Novo motivo");
        assertThat(updated.scheduledAt()).isEqualTo(slot);
    }

    @Test
    void updateRefreshesTheDenormalizedSnapshotFromTheRemoteService() {
        when(petClient.getPet(1L)).thenReturn(REX);
        AppointmentResponse created = service.create(request(1L, futureSlot(), "Dra. Helena"));

        // O pet mudou de tutor no monolito depois do agendamento
        when(petClient.getPet(1L)).thenReturn(new PetSummary(1L, "Rex", 9L, "Bruno Lima"));
        AppointmentResponse updated = service.update(created.id(),
                request(1L, futureSlot().plusDays(1), "Dra. Helena"));

        assertThat(updated.ownerId()).isEqualTo(9L);
        assertThat(updated.ownerName()).isEqualTo("Bruno Lima");
    }

    @Test
    void updateStatusMovesTheAppointmentThroughItsLifecycle() {
        when(petClient.getPet(1L)).thenReturn(REX);
        AppointmentResponse created = service.create(request(1L, futureSlot(), "Dra. Helena"));

        assertThat(service.updateStatus(created.id(), AppointmentStatus.COMPLETED).status())
                .isEqualTo(AppointmentStatus.COMPLETED);
    }

    @Test
    void findAllFiltersByPetOwnerAndStatus() {
        when(petClient.getPet(1L)).thenReturn(REX);
        when(petClient.getPet(2L)).thenReturn(new PetSummary(2L, "Mimi", 8L, "Bruno Lima"));
        AppointmentResponse rex = service.create(request(1L, futureSlot(), "Dra. Helena"));
        service.create(request(2L, futureSlot().plusDays(1), "Dr. Marcos"));
        service.updateStatus(rex.id(), AppointmentStatus.CANCELLED);

        assertThat(service.findAll(null, null, null)).hasSize(2);
        assertThat(service.findAll(1L, null, null)).hasSize(1);
        assertThat(service.findAll(null, 8L, null)).hasSize(1);
        assertThat(service.findAll(null, null, AppointmentStatus.CANCELLED)).hasSize(1);
        assertThat(service.findAll(null, null, AppointmentStatus.NO_SHOW)).isEmpty();
    }

    @Test
    void findByIdAndDeleteFailWhenTheAppointmentDoesNotExist() {
        assertThatThrownBy(() -> service.findById(999999L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Appointment not found: 999999");

        assertThatThrownBy(() -> service.delete(999999L))
                .isInstanceOf(NoSuchElementException.class);
    }

    private static LocalDateTime futureSlot() {
        return LocalDateTime.now().plusDays(30).withNano(0);
    }

    private static AppointmentRequest request(Long petId, LocalDateTime slot, String vet) {
        return new AppointmentRequest(petId, slot, vet, "Consulta de rotina", null);
    }

    /** Constrói a exceção que o Feign lançaria para um dado status HTTP do serviço remoto. */
    private static FeignException feignError(int status) {
        Request request = Request.create(Request.HttpMethod.GET, "/api/pets/1",
                Map.of(), null, StandardCharsets.UTF_8, null);
        Response response = Response.builder()
                .status(status)
                .reason("error")
                .request(request)
                .headers(Map.of())
                .build();
        return FeignException.errorStatus("PetClient#getPet(Long)", response);
    }
}
