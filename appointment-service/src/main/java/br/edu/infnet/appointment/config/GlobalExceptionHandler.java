package br.edu.infnet.appointment.config;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;
import java.util.stream.Collectors;

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

    /**
     * Payload inválido vira 400 com o motivo em texto puro, como os demais status.
     *
     * <p>Era o único buraco no contrato de erro: sem este handler o Spring devolve
     * seu JSON padrão, que — por omitir os binding errors — não diz nem qual campo
     * falhou. Como o frontend propaga o corpo da resposta direto para a tela, esse
     * JSON aparecia para o usuário no lugar da mensagem.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleValidation(MethodArgumentNotValidException ex) {
        return ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(" "));
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
