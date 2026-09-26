package br.edu.infnet.petclinic.events;

import br.edu.infnet.petclinic.events.dto.PetSnapshot;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "server.port=0",
        "petclinic.events.enabled=true",
        "spring.rabbitmq.listener.simple.auto-startup=false"
})
@TestPropertySource(properties = "spring.sql.init.mode=never")
class EventSerializationTest {

    @Autowired
    private MessageConverter messageConverter;

    @Test
    void theEnvelopeIsConvertedToJsonAndNotToJavaSerialization() {
        EventEnvelope<PetSnapshot> envelope = EventEnvelope.of(
                EventContract.PET_UPDATED, "pet", "1", "correlation-1",
                new PetSnapshot(1L, "Rex", "DOG", "Labrador", 7L, "Alice Souza"));

        Message message = messageConverter.toMessage(envelope, new MessageProperties());

        assertThat(message.getMessageProperties().getContentType())
                .isEqualTo(MessageProperties.CONTENT_TYPE_JSON);
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        // Nomes de campo do contrato — é isto que um consumidor em outro serviço lê
        assertThat(body)
                .contains("\"eventType\":\"pet.updated\"")
                .contains("\"version\":" + EventContract.SCHEMA_VERSION)
                .contains("\"correlationId\":\"correlation-1\"")
                .contains("\"ownerName\":\"Alice Souza\"");
    }
}
