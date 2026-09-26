package br.edu.infnet.appointment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Microsserviço do contexto delimitado <b>Scheduling</b>: agenda consultas veterinárias.
 *
 * <p>Mantém banco próprio ({@code appointmentsdb}) e nunca acessa as tabelas do monolito.
 * O caminho normal lê a projeção local alimentada por eventos; o {@code PetClient}
 * (OpenFeign) só entra como fallback, apontando para a URL do monolito injetada por
 * {@code BACKEND_URL}.
 */
@SpringBootApplication
@EnableFeignClients
public class AppointmentServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AppointmentServiceApplication.class, args);
	}

}
