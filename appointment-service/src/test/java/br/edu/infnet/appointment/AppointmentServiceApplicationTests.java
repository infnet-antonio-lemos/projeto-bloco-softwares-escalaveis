package br.edu.infnet.appointment;

import br.edu.infnet.appointment.client.PetClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Smoke test do contexto. O {@link PetClient} é substituído por um mock para que a
 * suíte não dependa do monolito nem do Eureka estarem no ar.
 */
@SpringBootTest(properties = "server.port=0")
class AppointmentServiceApplicationTests {

	@MockitoBean
	private PetClient petClient;

	@Test
	void contextLoads() {
	}

}
