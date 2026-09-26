package br.edu.infnet.appointment.events;

/** Nome do header e da chave de MDC usados para correlacionar requisição, logs e eventos. */
public final class CorrelationId {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    private CorrelationId() {
    }
}
