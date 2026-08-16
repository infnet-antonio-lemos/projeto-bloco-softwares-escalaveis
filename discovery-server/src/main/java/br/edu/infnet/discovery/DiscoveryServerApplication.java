package br.edu.infnet.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Service registry do Pet Clinic.
 *
 * <p>Os demais serviços (petclinic-backend, appointment-service, api-gateway) registram-se
 * aqui na subida e consultam o registro para resolver nomes lógicos em endereços de rede.
 * É isso que permite ao gateway rotear para {@code lb://petclinic-backend} e ao
 * appointment-service chamar o monolito por OpenFeign sem conhecer host nem porta.
 */
@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(DiscoveryServerApplication.class, args);
	}

}
