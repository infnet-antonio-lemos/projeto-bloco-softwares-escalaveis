package br.edu.infnet.petclinic.owner.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// As mensagens são frases completas porque o GlobalExceptionHandler devolve
// apenas o defaultMessage — sem o nome do campo — direto para a tela.
public record OwnerRequest(
    @NotBlank(message = "Informe o nome do tutor.")
    String name,

    @NotBlank(message = "Informe o e-mail do tutor.")
    @Email(message = "O e-mail informado não é válido.")
    String email,

    String phone,
    String address
) {}
