package br.edu.infnet.petclinic.events;

import br.edu.infnet.petclinic.owner.OwnerService;
import br.edu.infnet.petclinic.owner.dto.OwnerRequest;
import br.edu.infnet.petclinic.owner.dto.OwnerResponse;
import br.edu.infnet.petclinic.pet.PetService;
import br.edu.infnet.petclinic.pet.Species;
import br.edu.infnet.petclinic.pet.dto.PetRequest;
import br.edu.infnet.petclinic.pet.dto.PetResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Verifica o lado produtor: cada operação de domínio publica o evento certo, na routing
 * key certa, com o payload certo.
 *
 * <p>O {@code RabbitTemplate} é substituído por um mock — a suíte não precisa de broker,
 * mas também não prova que a mensagem chega ao destino. Essa parte cabe ao teste de
 * integração com Testcontainers do {@code appointment-service}.
 *
 * <p>Foi assim que o teste ficou depois que a <i>outbox</i> saiu. Antes, "publicar um
 * evento" tinha um efeito local — uma linha em tabela — que se verificava com um
 * {@code select}. Agora o efeito é uma chamada a um colaborador externo, e o teste
 * precisa de mock e de captor. É um custo real da simplificação, e vale conhecê-lo.
 */
@SpringBootTest(properties = {
        "server.port=0",
        "petclinic.events.enabled=true",
        // Só o publisher interessa aqui; sem isto os containers de listener
        // tentariam abrir conexão com um broker que não existe.
        "spring.rabbitmq.listener.simple.auto-startup=false"
})
@TestPropertySource(properties = "spring.sql.init.mode=never")
class EventPublishingTest {

    @Autowired
    private PetService petService;

    @Autowired
    private OwnerService ownerService;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @Test
    void creatingAnOwnerPublishesAnOwnerCreatedEvent() {
        OwnerResponse owner = ownerService.create(ownerRequest("publish-create@email.com"));

        // Routing key = tipo do evento: é o que permite ao consumidor assinar "owner.*"
        EventEnvelope<?> envelope = captureOne(EventContract.OWNER_CREATED);
        assertThat(envelope.aggregateType()).isEqualTo("owner");
        assertThat(envelope.aggregateId()).isEqualTo(String.valueOf(owner.id()));
    }

    @Test
    void petEventsCarryTheFullSnapshotIncludingTheOwnerName() {
        OwnerResponse owner = ownerService.create(ownerRequest("publish-pet@email.com"));
        PetResponse pet = petService.create(new PetRequest(
                "Rex", Species.DOG, "Labrador", LocalDate.of(2020, 3, 15), owner.id()));

        JsonNode payload = payloadOf(captureOne(EventContract.PET_CREATED));
        assertThat(payload.get("id").asLong()).isEqualTo(pet.id());
        assertThat(payload.get("name").asString()).isEqualTo("Rex");
        assertThat(payload.get("species").asString()).isEqualTo("DOG");
        // O nome do tutor viaja no evento para que o consumidor não precise consultar o
        // monolito de volta — é o que sustenta a projeção local do Scheduling.
        assertThat(payload.get("ownerId").asLong()).isEqualTo(owner.id());
        assertThat(payload.get("ownerName").asString()).isEqualTo("Tutor Publisher");
    }

    @Test
    void deletingAPetPublishesTheStateItHadBeforeBeingRemoved() {
        OwnerResponse owner = ownerService.create(ownerRequest("publish-delete@email.com"));
        PetResponse pet = petService.create(new PetRequest(
                "Mimi", Species.CAT, "Siamese", LocalDate.of(2019, 7, 20), owner.id()));

        petService.delete(pet.id());

        // Sem o snapshot capturado antes do delete, o evento diria apenas "algo sumiu"
        assertThat(payloadOf(captureOne(EventContract.PET_DELETED)).get("name").asString())
                .isEqualTo("Mimi");
    }

    @Test
    void deletingAnOwnerCarriesTheCascadedPetIds() {
        OwnerResponse owner = ownerService.create(ownerRequest("publish-cascade@email.com"));
        PetResponse first = petService.create(new PetRequest("Thor", Species.DOG, null, null, owner.id()));
        PetResponse second = petService.create(new PetRequest("Coco", Species.BIRD, null, null, owner.id()));

        ownerService.delete(owner.id());

        // Os pets somem em cascata sem gerar pet.deleted; os ids são a única pista que o
        // Scheduling tem para invalidar as projeções e cancelar as consultas.
        JsonNode payload = payloadOf(captureOne(EventContract.OWNER_DELETED));
        assertThat(payload.get("petIds").valueStream().map(JsonNode::asLong).toList())
                .containsExactlyInAnyOrder(first.id(), second.id());
    }

    @Test
    void everyEventCarriesAUniqueIdAndASchemaVersion() {
        ownerService.create(ownerRequest("publish-envelope-a@email.com"));
        ownerService.create(ownerRequest("publish-envelope-b@email.com"));

        List<EventEnvelope<?>> envelopes = captureAll(EventContract.OWNER_CREATED, 2);
        // eventId único é o que torna a deduplicação possível do lado consumidor
        assertThat(envelopes).extracting(EventEnvelope::eventId).doesNotHaveDuplicates();
        assertThat(envelopes).allSatisfy(envelope -> {
            assertThat(envelope.version()).isEqualTo(EventContract.SCHEMA_VERSION);
            assertThat(envelope.occurredAt()).isNotNull();
        });
    }

    /**
     * O envelope precisa sobreviver à serialização com os nomes de campo do contrato —
     * é isso que um consumidor em outro serviço (ou em outra linguagem) vai ler.
     */
    @Test
    void theSerializedEnvelopeMatchesThePublishedContract() {
        ownerService.create(ownerRequest("publish-contract@email.com"));

        JsonNode json = jsonMapper.valueToTree(captureOne(EventContract.OWNER_CREATED));
        assertThat(json.propertyNames()).contains(
                "eventId", "eventType", "occurredAt", "aggregateType",
                "aggregateId", "version", "correlationId", "payload");
        assertThat(json.get("eventType").asString()).isEqualTo(EventContract.OWNER_CREATED);
    }

    // --- captura --------------------------------------------------------------

    private EventEnvelope<?> captureOne(String eventType) {
        return captureAll(eventType, 1).getFirst();
    }

    private List<EventEnvelope<?>> captureAll(String eventType, int expected) {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate, times(expected)).convertAndSend(
                eq(EventContract.REGISTRY_EXCHANGE), eq(eventType),
                captor.capture(), any(MessagePostProcessor.class));
        return captor.getAllValues().stream()
                .map(v -> (EventEnvelope<?>) v)
                .collect(java.util.stream.Collectors.<EventEnvelope<?>>toList());
    }

    private JsonNode payloadOf(EventEnvelope<?> envelope) {
        return jsonMapper.valueToTree(envelope.payload());
    }

    private OwnerRequest ownerRequest(String email) {
        return new OwnerRequest("Tutor Publisher", email, "(11) 90000-0000", "Rua do Evento, 1");
    }
}
