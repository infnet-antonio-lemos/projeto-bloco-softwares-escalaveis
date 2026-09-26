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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@SpringBootTest(properties = {
        "server.port=0",
        // Os eventos emitidos em cascata são o objeto de metade destes testes, então a
        // mensageria fica ligada — com o RabbitTemplate mockado no lugar do broker.
        "petclinic.events.enabled=true",
        "spring.rabbitmq.listener.simple.auto-startup=false"
})
@TestPropertySource(properties = "spring.sql.init.mode=never")
class RegistryEventHandlerTest {

    @Autowired
    private RegistryEventHandler handler;

    @Autowired
    private PetViewRepository petViews;

    @Autowired
    private AppointmentRepository appointments;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void clean() {
        appointments.deleteAll();
        petViews.deleteAll();
    }

    @Test
    void petCreatedPopulatesTheLocalProjection() {
        handler.onPetEvent(petEvent(EventContract.PET_CREATED, rex("Rex", 7L, "Alice Souza"), Instant.now()));

        PetView view = petViews.findById(1L).orElseThrow();
        assertThat(view.getName()).isEqualTo("Rex");
        assertThat(view.getOwnerName()).isEqualTo("Alice Souza");
    }

    @Test
    void petUpdatedSyncsScheduledAppointmentsButNotHistoricalOnes() {
        Appointment scheduled = appointments.save(appointment("Rex", "Alice Souza", AppointmentStatus.SCHEDULED));
        Appointment completed = appointments.save(appointment("Rex", "Alice Souza", AppointmentStatus.COMPLETED));

        handler.onPetEvent(petEvent(EventContract.PET_UPDATED, rex("Rexinho", 9L, "Bruno Lima"), Instant.now()));

        assertThat(appointments.findById(scheduled.getId()).orElseThrow().getPetName()).isEqualTo("Rexinho");
        assertThat(appointments.findById(scheduled.getId()).orElseThrow().getOwnerName()).isEqualTo("Bruno Lima");
        // Consulta já realizada é registro histórico: preserva o nome vigente na época
        assertThat(appointments.findById(completed.getId()).orElseThrow().getPetName()).isEqualTo("Rex");
    }

    @Test
    void petDeletedCancelsScheduledAppointmentsAndAnnouncesEachCancellation() {
        Appointment scheduled = appointments.save(appointment("Rex", "Alice Souza", AppointmentStatus.SCHEDULED));

        handler.onPetEvent(petEvent(EventContract.PET_DELETED, rex("Rex", 7L, "Alice Souza"), Instant.now()));

        assertThat(appointments.findById(scheduled.getId()).orElseThrow().getStatus())
                .isEqualTo(AppointmentStatus.CANCELLED);
        // Sem tombstone: a linha some da projeção
        assertThat(petViews.findById(1L)).isEmpty();

        // A cascata é anunciada: o evento é publicado e alimentará as notificações
        AppointmentSnapshot payload = (AppointmentSnapshot) captureCancellations(1).getFirst().payload();
        assertThat(payload.trigger()).isEqualTo(AppointmentSnapshot.TRIGGER_PET_DELETED);
    }

    @Test
    void ownerDeletedCancelsEveryAppointmentOfThatOwner() {
        appointments.save(appointment("Rex", "Alice Souza", AppointmentStatus.SCHEDULED));
        appointments.save(appointment("Mimi", "Alice Souza", AppointmentStatus.SCHEDULED));
        petViews.save(PetView.builder().id(1L).name("Rex").ownerId(7L).ownerName("Alice Souza").build());

        handler.onOwnerEvent(ownerEvent(EventContract.OWNER_DELETED,
                new OwnerSnapshot(7L, "Alice Souza", "alice@email.com", null, List.of(1L)), Instant.now()));

        assertThat(appointments.findAll())
                .allSatisfy(a -> assertThat(a.getStatus()).isEqualTo(AppointmentStatus.CANCELLED));
        assertThat(petViews.findById(1L)).isEmpty();
        assertThat(captureCancellations(2)).hasSize(2);
    }

    @Test
    void ownerUpdatedPropagatesTheNewNameToProjectionAndScheduledAppointments() {
        petViews.save(PetView.builder().id(1L).name("Rex").ownerId(7L).ownerName("Alice Souza").build());
        Appointment scheduled = appointments.save(appointment("Rex", "Alice Souza", AppointmentStatus.SCHEDULED));

        handler.onOwnerEvent(ownerEvent(EventContract.OWNER_UPDATED,
                new OwnerSnapshot(7L, "Alice S. Mendes", "alice@email.com", null, List.of(1L)), Instant.now()));

        assertThat(petViews.findById(1L).orElseThrow().getOwnerName()).isEqualTo("Alice S. Mendes");
        assertThat(appointments.findById(scheduled.getId()).orElseThrow().getOwnerName())
                .isEqualTo("Alice S. Mendes");
    }

    @Test
    void redeliveryOfTheSameEventHasNoAdditionalEffect() {
        Appointment scheduled = appointments.save(appointment("Rex", "Alice Souza", AppointmentStatus.SCHEDULED));
        PetEventMessage event = petEvent(EventContract.PET_DELETED, rex("Rex", 7L, "Alice Souza"), Instant.now());

        handler.onPetEvent(event);
        handler.onPetEvent(event);

        assertThat(appointments.findById(scheduled.getId()).orElseThrow().getStatus())
                .isEqualTo(AppointmentStatus.CANCELLED);
        // Uma única publicação, sem nenhuma marca de idempotência no caminho
        assertThat(captureCancellations(1)).hasSize(1);
    }

    @Test
    void aLateUpdateResurrectsAPetThatWasDeleted() {
        Instant deletion = Instant.now();
        handler.onPetEvent(petEvent(EventContract.PET_CREATED, rex("Rex", 7L, "Alice Souza"),
                deletion.minus(2, ChronoUnit.HOURS)));
        handler.onPetEvent(petEvent(EventContract.PET_DELETED, rex("Rex", 7L, "Alice Souza"), deletion));
        assertThat(petViews.findById(1L)).isEmpty();

        // Evento mais ANTIGO que a exclusão, entregue depois dela
        handler.onPetEvent(petEvent(EventContract.PET_UPDATED, rex("Rex", 7L, "Alice Souza"),
                deletion.minus(1, ChronoUnit.HOURS)));

        assertThat(petViews.findById(1L)).isPresent();
    }

    @Test
    void anEventOlderThanTheProjectionIsDiscarded() {
        Instant now = Instant.now();
        handler.onPetEvent(petEvent(EventContract.PET_UPDATED, rex("Nome novo", 7L, "Alice Souza"), now));
        handler.onPetEvent(petEvent(EventContract.PET_UPDATED, rex("Nome antigo", 7L, "Alice Souza"),
                now.minus(1, ChronoUnit.HOURS)));

        assertThat(petViews.findById(1L).orElseThrow().getName()).isEqualTo("Nome novo");
    }

    @Test
    void anUnknownEventTypeIsIgnoredWithoutFailing() {
        handler.onPetEvent(petEvent("pet.microchipped", rex("Rex", 7L, "Alice Souza"), Instant.now()));

        assertThat(petViews.findAll()).isEmpty();
        verify(rabbitTemplate, never()).convertAndSend(
                eq(EventContract.SCHEDULING_EXCHANGE), any(String.class),
                any(Object.class), any(MessagePostProcessor.class));
    }

    /** Captura os {@code appointment.cancelled} publicados, falhando se a contagem diferir. */
    private List<EventEnvelope<?>> captureCancellations(int expected) {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate, times(expected)).convertAndSend(
                eq(EventContract.SCHEDULING_EXCHANGE), eq(EventContract.APPOINTMENT_CANCELLED),
                captor.capture(), any(MessagePostProcessor.class));
        return captor.getAllValues().stream()
                .map(v -> (EventEnvelope<?>) v)
                .collect(java.util.stream.Collectors.<EventEnvelope<?>>toList());
    }

    // --- fixtures ----------------------------------------------------------

    private static PetSnapshot rex(String name, Long ownerId, String ownerName) {
        return new PetSnapshot(1L, name, "DOG", "Labrador", ownerId, ownerName);
    }

    private static PetEventMessage petEvent(String type, PetSnapshot payload, Instant occurredAt) {
        return new PetEventMessage(UUID.randomUUID().toString(), type, occurredAt,
                String.valueOf(payload.id()), EventContract.SCHEMA_VERSION, "test-correlation", payload);
    }

    private static OwnerEventMessage ownerEvent(String type, OwnerSnapshot payload, Instant occurredAt) {
        return new OwnerEventMessage(UUID.randomUUID().toString(), type, occurredAt,
                String.valueOf(payload.id()), EventContract.SCHEMA_VERSION, "test-correlation", payload);
    }

    private static Appointment appointment(String petName, String ownerName, AppointmentStatus status) {
        return Appointment.builder()
                .petId(1L)
                .ownerId(7L)
                .petName(petName)
                .ownerName(ownerName)
                .scheduledAt(LocalDateTime.now().plusDays(10).withNano(0))
                .veterinarian("Dra. Helena")
                .reason("Consulta de rotina")
                .status(status)
                .build();
    }
}
