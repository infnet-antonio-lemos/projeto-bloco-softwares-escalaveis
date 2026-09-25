package br.edu.infnet.appointment.events;

import br.edu.infnet.appointment.appointment.AppointmentRepository;
import br.edu.infnet.appointment.appointment.AppointmentService;
import br.edu.infnet.appointment.appointment.AppointmentStatus;
import br.edu.infnet.appointment.appointment.dto.AppointmentRequest;
import br.edu.infnet.appointment.appointment.dto.AppointmentResponse;
import br.edu.infnet.appointment.events.dto.AppointmentSnapshot;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = {
        "server.port=0",
        "petclinic.events.enabled=true",
        "spring.rabbitmq.listener.simple.auto-startup=false"
})
@TestPropertySource(properties = "spring.sql.init.mode=never")
class AppointmentEventPublishingTest {

    @Autowired
    private AppointmentService service;

    @Autowired
    private AppointmentRepository appointments;

    @Autowired
    private PetViewRepository petViews;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void clean() {
        appointments.deleteAll();
        petViews.deleteAll();
        petViews.save(PetView.builder()
                .id(1L).name("Rex").ownerId(7L).ownerName("Alice Souza").build());
    }

    @Test
    void creatingAnAppointmentAnnouncesItScheduled() {
        AppointmentResponse created = service.create(request(futureSlot(), "Dra. Helena"));

        EventEnvelope<?> envelope = captureOne(EventContract.APPOINTMENT_SCHEDULED);
        assertThat(envelope.aggregateId()).isEqualTo(String.valueOf(created.id()));

        AppointmentSnapshot payload = (AppointmentSnapshot) envelope.payload();
        // Nomes viajam no evento: o serviço de notificações monta o aviso sem consultar
        // nem o agendamento nem o cadastro.
        assertThat(payload.petName()).isEqualTo("Rex");
        assertThat(payload.ownerName()).isEqualTo("Alice Souza");
        assertThat(payload.trigger()).isEqualTo(AppointmentSnapshot.TRIGGER_USER);
    }

    @Test
    void changingTheSlotAnnouncesARescheduleCarryingThePreviousTime() {
        LocalDateTime original = futureSlot();
        AppointmentResponse created = service.create(request(original, "Dra. Helena"));

        service.update(created.id(), request(original.plusDays(2), "Dra. Helena"));

        AppointmentSnapshot payload =
                (AppointmentSnapshot) captureOne(EventContract.APPOINTMENT_RESCHEDULED).payload();
        // O horário anterior só existe no evento — o banco já foi sobrescrito. Sem ele,
        // o aviso não conseguiria dizer ao tutor o que mudou.
        assertThat(payload.previousScheduledAt()).isEqualTo(original);
    }

    @Test
    void editingOnlyTheDetailsUsesADifferentRoutingKeyThanAReschedule() {
        LocalDateTime slot = futureSlot();
        AppointmentResponse created = service.create(request(slot, "Dra. Helena"));

        service.update(created.id(), new AppointmentRequest(
                1L, slot, "Dra. Helena", "Outro motivo", "Observação nova"));

        // Corrigir uma observação não deve disparar aviso de remarcação ao tutor
        captureOne(EventContract.APPOINTMENT_UPDATED);
    }

    @Test
    void eachFinalStatusHasItsOwnEventType() {
        AppointmentResponse created = service.create(request(futureSlot(), "Dra. Helena"));

        service.updateStatus(created.id(), AppointmentStatus.COMPLETED);
        service.updateStatus(created.id(), AppointmentStatus.NO_SHOW);
        service.updateStatus(created.id(), AppointmentStatus.CANCELLED);

        captureOne(EventContract.APPOINTMENT_COMPLETED);
        captureOne(EventContract.APPOINTMENT_NO_SHOW);
        captureOne(EventContract.APPOINTMENT_CANCELLED);
    }

    @Test
    void deletingAnAppointmentAnnouncesItBeforeTheRowIsGone() {
        AppointmentResponse created = service.create(request(futureSlot(), "Dra. Helena"));

        service.delete(created.id());

        AppointmentSnapshot payload =
                (AppointmentSnapshot) captureOne(EventContract.APPOINTMENT_DELETED).payload();
        assertThat(payload.petName()).isEqualTo("Rex");
    }

    // --- captura --------------------------------------------------------------

    private EventEnvelope<?> captureOne(String eventType) {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(EventContract.SCHEDULING_EXCHANGE), eq(eventType),
                captor.capture(), any(MessagePostProcessor.class));
        List<Object> values = captor.getAllValues();
        return (EventEnvelope<?>) values.getLast();
    }

    private static LocalDateTime futureSlot() {
        return LocalDateTime.now().plusDays(30).withNano(0);
    }

    private static AppointmentRequest request(LocalDateTime slot, String vet) {
        return new AppointmentRequest(1L, slot, vet, "Consulta de rotina", null);
    }
}
