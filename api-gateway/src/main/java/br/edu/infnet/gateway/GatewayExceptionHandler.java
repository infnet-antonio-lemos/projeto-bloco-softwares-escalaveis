package br.edu.infnet.gateway;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

/**
 * Traduz falhas de roteamento em respostas úteis ao cliente.
 *
 * <p>Sem isto, um serviço fora do ar (ou ainda não registrado no Eureka) faz o
 * LoadBalancer lançar {@code HttpServerErrorException} e o Tomcat devolver uma
 * página HTML de erro 500 — que o frontend exibe como falha genérica, escondendo
 * a causa real. Aqui a resposta vira um 503 em texto puro, consistente com o
 * contrato de erro dos demais serviços.
 */
@RestControllerAdvice
public class GatewayExceptionHandler {

    @ExceptionHandler(HttpServerErrorException.class)
    public ResponseEntity<String> handleNoInstance(HttpServerErrorException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Serviço indisponível no momento: " + ex.getMessage());
    }

    /** O serviço está registrado, mas a conexão falhou (parando, rede caiu). */
    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<String> handleUnreachable(ResourceAccessException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Serviço não respondeu: " + ex.getMessage());
    }
}
