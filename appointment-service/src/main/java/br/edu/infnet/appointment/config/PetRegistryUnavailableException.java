package br.edu.infnet.appointment.config;

/**
 * O serviço de cadastro de pets (monolito) não respondeu.
 *
 * <p>Distingue "o pet não existe" (que vira 404) de "não consegui perguntar"
 * (que vira 503): sem essa separação, uma queda do serviço dependente seria
 * reportada ao cliente como se o pet não existisse.
 */
public class PetRegistryUnavailableException extends RuntimeException {

    public PetRegistryUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
