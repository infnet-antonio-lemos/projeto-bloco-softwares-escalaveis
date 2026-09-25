package br.edu.infnet.appointment.events;

import br.edu.infnet.appointment.appointment.AppointmentRepository;
import br.edu.infnet.appointment.appointment.AppointmentService;
import br.edu.infnet.appointment.appointment.dto.AppointmentRequest;
import br.edu.infnet.appointment.projection.PetView;
import br.edu.infnet.appointment.projection.PetViewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(properties = {
        "server.port=0",
        "spring.sql.init.mode=never",
        "petclinic.events.enabled=true"
})
@Testcontainers
@EnabledIf("dockerAvailable")
class EventMessagingIntegrationTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(20);
    private static final String SUBSCRIBER_QUEUE = "test.scheduling-subscriber";

    @Container
    static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:4-management-alpine");

    static boolean dockerAvailable() {
        return DockerClientFactory.instance().isDockerAvailable();
    }

    @DynamicPropertySource
    static void rabbitProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", RABBIT::getHost);
        registry.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBIT::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBIT::getAdminPassword);
    }

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private AppointmentRepository appointments;

    @Autowired
    private PetViewRepository petViews;

    @BeforeEach
    void clean() {
        appointments.deleteAll();
        petViews.deleteAll();
        drain(SUBSCRIBER_QUEUE);
    }

    @Test
    void aPetEventPublishedByTheRegistryUpdatesTheLocalProjection() {
        publishRaw(EventContract.REGISTRY_EXCHANGE, EventContract.PET_UPDATED, petEventJson(
                UUID.randomUUID().toString(), "Rex Atualizado"));

        await().atMost(TIMEOUT).untilAsserted(() ->
                assertThat(petViews.findById(42L))
                        .get()
                        .extracting(PetView::getName)
                        .isEqualTo("Rex Atualizado"));
    }

    @Test
    void anAppointmentEventReachesAnExternalSubscriber() {
        declareSubscriber();
        petViews.save(PetView.builder()
                .id(42L).name("Rex").ownerId(7L).ownerName("Alice Souza").build());

        appointmentService.create(new AppointmentRequest(
                42L, LocalDateTime.now().plusDays(20).withNano(0), "Dra. Helena", "Check-up", null));

        Message received = await().atMost(TIMEOUT).until(
                () -> rabbitTemplate.receive(SUBSCRIBER_QUEUE), m -> m != null);

        MessageProperties properties = received.getMessageProperties();
        assertThat(properties.getReceivedRoutingKey()).isEqualTo(EventContract.APPOINTMENT_SCHEDULED);
        assertThat(properties.getContentType()).isEqualTo(MessageProperties.CONTENT_TYPE_JSON);
        // Headers técnicos permitem filtrar e auditar sem abrir o corpo da mensagem
        assertThat(properties.getHeaders()).containsEntry("x-event-type", EventContract.APPOINTMENT_SCHEDULED);
        assertThat(new String(received.getBody(), StandardCharsets.UTF_8))
                .contains("\"petName\":\"Rex\"")
                .contains("\"ownerName\":\"Alice Souza\"");
    }

    @Test
    void anUnreadableEventIsDiscardedWithoutBlockingTheQueue() {
        publishRaw(EventContract.REGISTRY_EXCHANGE, EventContract.PET_UPDATED,
                "{\"eventId\":\"veneno\",\"eventType\":\"pet.updated\",\"payload\":{");

        await().atMost(TIMEOUT).untilAsserted(() -> {
            QueueInformation info = amqpAdmin.getQueueInfo(EventContract.PET_EVENTS_QUEUE);
            assertThat(info).isNotNull();
            assertThat(info.getMessageCount()).isZero();
        });

        // Nada foi aplicado a partir da mensagem ilegível...
        assertThat(petViews.findById(42L)).isEmpty();

        // ...e a fila seguiu viva para a próxima
        publishRaw(EventContract.REGISTRY_EXCHANGE, EventContract.PET_UPDATED,
                petEventJson(UUID.randomUUID().toString(), "Rex Depois do Veneno"));

        await().atMost(TIMEOUT).untilAsserted(() ->
                assertThat(petViews.findById(42L))
                        .get()
                        .extracting(PetView::getName)
                        .isEqualTo("Rex Depois do Veneno"));
    }

    // --- infraestrutura do teste -------------------------------------------

    /** Assinante externo, criado como qualquer novo consumidor faria: fila + binding. */
    private void declareSubscriber() {
        Queue queue = QueueBuilder.durable(SUBSCRIBER_QUEUE).build();
        amqpAdmin.declareQueue(queue);
        amqpAdmin.declareBinding(BindingBuilder.bind(queue)
                .to(new TopicExchange(EventContract.SCHEDULING_EXCHANGE, true, false))
                .with(EventContract.APPOINTMENT_ROUTING_PATTERN));
    }

    private void publishRaw(String exchange, String routingKey, String json) {
        rabbitTemplate.send(exchange, routingKey, MessageBuilder
                .withBody(json.getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .build());
    }

    private void drain(String queue) {
        try {
            while (rabbitTemplate.receive(queue) != null) {
                // esvazia resíduo do teste anterior
            }
        } catch (RuntimeException ex) {
            // fila ainda não declarada neste ponto do ciclo — nada a limpar
        }
    }

    private static String petEventJson(String eventId, String petName) {
        return """
                {"eventId":"%s","eventType":"pet.updated","occurredAt":"%s",
                 "aggregateType":"pet","aggregateId":"42","version":1,
                 "correlationId":"it-correlation",
                 "payload":{"id":42,"name":"%s","species":"DOG","breed":"Labrador",
                            "ownerId":7,"ownerName":"Alice Souza"}}
                """.formatted(eventId, Instant.now().toString(), petName).replace("\n", "");
    }
}
