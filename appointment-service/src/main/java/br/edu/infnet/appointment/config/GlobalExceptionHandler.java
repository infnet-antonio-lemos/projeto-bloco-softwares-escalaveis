package br.edu.infnet.appointment.config;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

/**
 * Mantém o mesmo contrato de erro do monolito (corpo em texto puro),
 * acrescentando o 503 específico da comunicação entre serviços.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(NoSuchElementException ex) {
        return ex.getMessage();
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handleConflict(DataIntegrityViolationException ex) {
        return "Registro conflita com dados já existentes (violação de restrição única).";
    }

    /**
     * Regra de negócio violada (ex.: veterinário já ocupado no horário) vira 409.
     */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handleBusinessConflict(IllegalStateException ex) {
        return ex.getMessage();
    }

    /**
     * Serviço dependente fora do ar vira 503 — o cliente sabe que pode tentar de novo.
     */
    @ExceptionHandler(PetRegistryUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public String handleUnavailable(PetRegistryUnavailableException ex) {
        return ex.getMessage();
    }
}
