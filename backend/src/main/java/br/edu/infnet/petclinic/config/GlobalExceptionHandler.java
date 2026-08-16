package br.edu.infnet.petclinic.config;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;
import java.util.stream.Collectors;

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
     * <p>O JSON de erro padrão do Spring omite os binding errors, então não informa
     * qual campo falhou — e o frontend, que mostra o corpo da resposta ao usuário,
     * acabava exibindo esse JSON. Mesmo tratamento existe no appointment-service.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleValidation(MethodArgumentNotValidException ex) {
        return ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(" "));
    }

    /**
     * Violação de restrição de integridade (ex.: e-mail de Owner duplicado,
     * que é UNIQUE) vira 409 Conflict em vez do 500 genérico.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handleConflict(DataIntegrityViolationException ex) {
        return "Registro conflita com dados já existentes (violação de restrição única).";
    }
}
