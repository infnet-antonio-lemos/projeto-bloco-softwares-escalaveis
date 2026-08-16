package br.edu.infnet.appointment.appointment;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Raiz do agregado Consulta.
 *
 * <p>{@code petId} e {@code ownerId} são referências para entidades de <b>outro</b>
 * contexto delimitado, mantidas como identificadores simples — sem {@code @ManyToOne},
 * sem chave estrangeira, sem join. É o que mantém os dois bancos independentes.
 *
 * <p>{@code petName} e {@code ownerName} são um snapshot capturado via Feign no momento
 * do agendamento. A denormalização é intencional: listar consultas não dispara N chamadas
 * HTTP ao monolito, e o histórico preserva o nome vigente na época da consulta.
 */
@Entity
@Table(name = "appointments")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long petId;

    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false)
    private String petName;

    private String ownerName;

    @Column(nullable = false)
    private LocalDateTime scheduledAt;

    @Column(nullable = false)
    private String veterinarian;

    private String reason;

    @Column(length = 1000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
