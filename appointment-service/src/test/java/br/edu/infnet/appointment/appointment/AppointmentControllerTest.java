package br.edu.infnet.appointment.appointment;

import br.edu.infnet.appointment.client.PetClient;
import br.edu.infnet.appointment.client.dto.PetSummary;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa os endpoints REST do microsserviço ponta a ponta (MockMvc), incluindo os
 * códigos de status traduzidos pelo GlobalExceptionHandler. O {@link PetClient} é
 * mockado para isolar do monolito.
 */
@SpringBootTest(properties = "server.port=0")
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.sql.init.mode=never")
class AppointmentControllerTest {

    private static final PetSummary REX = new PetSummary(1L, "Rex", 7L, "Alice Souza");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppointmentRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PetClient petClient;

    @BeforeEach
    void clean() {
        repository.deleteAll();
        when(petClient.getPet(1L)).thenReturn(REX);
    }

    @Test
    void postCreatesAppointmentWithDataResolvedFromTheRemoteService() throws Exception {
        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(1L, futureSlot(), "Dra. Helena")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.petName").value("Rex"))
                .andExpect(jsonPath("$.ownerId").value(7))
                .andExpect(jsonPath("$.ownerName").value("Alice Souza"))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
    }

    @Test
    void getListAndGetByIdReturnPersistedAppointments() throws Exception {
        long id = createAppointment();

        mockMvc.perform(get("/api/appointments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value((int) id));

        mockMvc.perform(get("/api/appointments/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.veterinarian").value("Dra. Helena"));
    }

    @Test
    void getListFiltersByPetOwnerAndStatus() throws Exception {
        createAppointment();

        mockMvc.perform(get("/api/appointments").param("petId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/appointments").param("ownerId", "7"))
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/appointments").param("status", "SCHEDULED"))
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/appointments").param("status", "CANCELLED"))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void putUpdatesTheAppointment() throws Exception {
        long id = createAppointment();

        mockMvc.perform(put("/api/appointments/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(1L, futureSlot().plusDays(2), "Dr. Marcos")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.veterinarian").value("Dr. Marcos"));
    }

    @Test
    void patchStatusMovesTheAppointmentThroughItsLifecycle() throws Exception {
        long id = createAppointment();

        mockMvc.perform(patch("/api/appointments/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void deleteRemovesTheAppointment() throws Exception {
        long id = createAppointment();

        mockMvc.perform(delete("/api/appointments/{id}", id))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/appointments/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void unknownAppointmentReturns404() throws Exception {
        mockMvc.perform(get("/api/appointments/{id}", 999999))
                .andExpect(status().isNotFound());
    }

    @Test
    void unknownPetInTheRemoteServiceReturns404() throws Exception {
        when(petClient.getPet(any())).thenThrow(feignError(404));

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(999L, futureSlot(), "Dra. Helena")))
                .andExpect(status().isNotFound());
    }

    @Test
    void remoteServiceOutageReturns503() throws Exception {
        when(petClient.getPet(any())).thenThrow(feignError(500));

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(1L, futureSlot(), "Dra. Helena")))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void doubleBookingTheSameVetReturns409() throws Exception {
        LocalDateTime slot = futureSlot();
        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(1L, slot, "Dra. Helena")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(1L, slot, "Dra. Helena")))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidPayloadsAreRejectedWith400() throws Exception {
        // Data no passado viola @Future
        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(1L, LocalDateTime.now().minusDays(1), "Dra. Helena")))
                .andExpect(status().isBadRequest());

        // Veterinário em branco viola @NotBlank
        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(1L, futureSlot(), "   ")))
                .andExpect(status().isBadRequest());

        // petId ausente viola @NotNull
        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(null, futureSlot(), "Dra. Helena")))
                .andExpect(status().isBadRequest());
    }

    private long createAppointment() throws Exception {
        String json = mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(1L, futureSlot(), "Dra. Helena")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asLong();
    }

    private static LocalDateTime futureSlot() {
        return LocalDateTime.now().plusDays(30).withNano(0);
    }

    private static String body(Long petId, LocalDateTime slot, String vet) {
        return """
                {"petId":%s,"scheduledAt":"%s","veterinarian":"%s","reason":"Consulta de rotina"}
                """.formatted(
                petId == null ? "null" : petId,
                slot.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                vet);
    }

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
