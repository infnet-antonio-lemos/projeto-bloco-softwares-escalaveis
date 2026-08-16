package br.edu.infnet.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.server.mvc.config.GatewayMvcProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Além do context load, verifica que as duas rotas foram realmente lidas do
 * application.properties. Sem esta asserção, um erro no prefixo das propriedades
 * (que mudou entre Gateway 4.x e 5.x) passaria despercebido: o gateway subiria
 * normalmente, apenas sem rota nenhuma, e a falha só apareceria em runtime.
 */
@SpringBootTest(properties = "server.port=0")
class ApiGatewayApplicationTests {

	@Autowired
	private GatewayMvcProperties properties;

	@Test
	void routesAreLoadedFromConfiguration() {
		assertThat(properties.getRoutes())
				.extracting(route -> route.getId() + " -> " + route.getUri())
				.containsExactlyInAnyOrder(
						"petclinic-backend -> lb://petclinic-backend",
						"appointment-service -> lb://appointment-service");
	}

}
