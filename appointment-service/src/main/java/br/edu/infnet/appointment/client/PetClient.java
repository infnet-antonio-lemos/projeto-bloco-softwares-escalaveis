package br.edu.infnet.appointment.client;

import br.edu.infnet.appointment.client.dto.PetSummary;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Cliente HTTP declarativo para o cadastro de pets do monolito.
 *
 * <p>Usado apenas como fallback: o caminho normal lê a projeção local alimentada por
 * eventos. Ver {@code projection.PetDirectory}.
 *
 * <p>A {@code url} é injetada por ambiente. Em Kubernetes vale
 * {@code http://backend:8081} — nome de Service, que o DNS do cluster resolve para o
 * ClusterIP e o kube-proxy balanceia entre os pods. É a plataforma que descobre e
 * balanceia, não a aplicação: o {@code name} aqui só nomeia o bean do cliente.
 */
@FeignClient(name = "petclinic-backend", url = "${BACKEND_URL:http://localhost:8081}", path = "/api/pets")
public interface PetClient {

    @GetMapping("/{id}")
    PetSummary getPet(@PathVariable("id") Long id);
}
