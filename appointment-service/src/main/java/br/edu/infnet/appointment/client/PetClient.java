package br.edu.infnet.appointment.client;

import br.edu.infnet.appointment.client.dto.PetSummary;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Cliente HTTP declarativo para o cadastro de pets do monolito.
 *
 * <p>O {@code name} é o nome lógico registrado no Eureka ({@code spring.application.name}
 * do monolito), não um host: o Spring Cloud LoadBalancer resolve para uma instância
 * concreta em tempo de chamada. Trocar a porta ou escalar o monolito não exige
 * nenhuma mudança aqui.
 */
@FeignClient(name = "petclinic-backend", path = "/api/pets")
public interface PetClient {

    @GetMapping("/{id}")
    PetSummary getPet(@PathVariable("id") Long id);
}
