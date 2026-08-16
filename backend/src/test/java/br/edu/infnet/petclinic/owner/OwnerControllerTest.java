package br.edu.infnet.petclinic.owner;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CRUD do Owner via HTTP, incluindo os status traduzidos pelo GlobalExceptionHandler
 * (404 para inexistente, 409 para e-mail duplicado) — que até então só eram
 * exercitados indiretamente pela fatia de repositório.
 *
 * <p>Cada teste usa e-mails próprios para não colidir com o seed de data.sql
 * nem com os demais testes, já que o contexto é compartilhado.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OwnerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void postCreatesOwnerAndReturns201() throws Exception {
        mockMvc.perform(post("/api/owners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Diego Alves", "diego.crud@email.com")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Diego Alves"))
                .andExpect(jsonPath("$.petCount").value(0));
    }

    @Test
    void getListIncludesTheSeededOwners() throws Exception {
        mockMvc.perform(get("/api/owners"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").isString());
    }

    @Test
    void putUpdatesTheOwner() throws Exception {
        long id = createOwner("Elena Rocha", "elena.crud@email.com");

        mockMvc.perform(put("/api/owners/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Elena Rocha Silva", "elena.crud@email.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Elena Rocha Silva"));
    }

    @Test
    void deleteRemovesTheOwner() throws Exception {
        long id = createOwner("Fabio Nunes", "fabio.crud@email.com");

        mockMvc.perform(delete("/api/owners/{id}", id))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/owners/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void unknownOwnerReturns404() throws Exception {
        mockMvc.perform(get("/api/owners/{id}", 999999))
                .andExpect(status().isNotFound());
    }

    @Test
    void duplicateEmailReturns409() throws Exception {
        createOwner("Gabriela Dias", "gabriela.dup@email.com");

        // O e-mail é UNIQUE: o GlobalExceptionHandler converte a violação em 409
        mockMvc.perform(post("/api/owners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Outra Pessoa", "gabriela.dup@email.com")))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidPayloadsAreRejectedWith400() throws Exception {
        mockMvc.perform(post("/api/owners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("", "sem.nome@email.com")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/owners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Email Invalido", "nao-e-um-email")))
                .andExpect(status().isBadRequest());
    }

    private long createOwner(String name, String email) throws Exception {
        String json = mockMvc.perform(post("/api/owners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(name, email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asLong();
    }

    private static String body(String name, String email) {
        return """
                {"name":"%s","email":"%s","phone":"(11) 90000-0000","address":"Rua Teste, 1"}
                """.formatted(name, email);
    }
}
