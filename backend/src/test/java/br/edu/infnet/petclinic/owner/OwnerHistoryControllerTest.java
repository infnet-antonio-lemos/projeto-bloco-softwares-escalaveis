package br.edu.infnet.petclinic.owner;

import br.edu.infnet.petclinic.owner.dto.OwnerRequest;
import br.edu.infnet.petclinic.owner.dto.OwnerResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa o endpoint REST de histórico do Owner ponta a ponta (MockMvc).
 * Não é @Transactional para que cada operação gere uma revisão Envers distinta.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OwnerHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OwnerService ownerService;

    @Test
    void historyEndpointReturnsRevisions() throws Exception {
        OwnerResponse created = ownerService.create(
                new OwnerRequest("Peter Parker", "peter.history@email.com", null, null));
        ownerService.update(created.id(),
                new OwnerRequest("Spider Man", "peter.history@email.com", null, null));

        mockMvc.perform(get("/api/owners/{id}/history", created.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$[0].revisionType").value("INSERT"))
                .andExpect(jsonPath("$[0].state.name").value("Peter Parker"))
                .andExpect(jsonPath("$[1].revisionType").value("UPDATE"))
                .andExpect(jsonPath("$[1].state.name").value("Spider Man"));
    }

    @Test
    void historyEndpointReturns404WhenNoHistory() throws Exception {
        mockMvc.perform(get("/api/owners/{id}/history", 888_888L))
                .andExpect(status().isNotFound());
    }
}
