package br.edu.infnet.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto único de entrada da API.
 *
 * <p>O frontend passa a conhecer apenas este endereço: o gateway resolve, pelo Eureka,
 * qual serviço atende cada caminho ({@code /api/owners} e {@code /api/pets} vão para o
 * monolito, {@code /api/appointments} para o microsserviço). As rotas estão declaradas
 * em {@code application.properties}.
 */
@SpringBootApplication
public class ApiGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiGatewayApplication.class, args);
	}

}
