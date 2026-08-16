package br.edu.infnet.appointment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Microsserviço do contexto delimitado <b>Scheduling</b>: agenda consultas veterinárias.
 *
 * <p>Mantém banco próprio ({@code appointmentsdb}) e nunca acessa as tabelas do monolito.
 * Os dados do pet e do tutor são obtidos via HTTP pelo {@code PetClient} (OpenFeign),
 * que resolve o endereço do {@code petclinic-backend} pelo Eureka.
 */
@SpringBootApplication
@EnableFeignClients
public class AppointmentServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AppointmentServiceApplication.class, args);
	}

}
