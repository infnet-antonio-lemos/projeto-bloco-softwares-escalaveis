package br.edu.infnet.discovery;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test: garante que o contexto do Eureka Server sobe.
 * Porta 0 evita colidir com uma instância já rodando na 8761.
 */
@SpringBootTest(properties = "server.port=0")
class DiscoveryServerApplicationTests {

	@Test
	void contextLoads() {
	}

}
