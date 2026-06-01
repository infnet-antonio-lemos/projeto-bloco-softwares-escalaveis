package br.edu.infnet.petclinic.owner.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record OwnerRequest(
    @NotBlank String name,
    @NotBlank @Email String email,
    String phone,
    String address
) {}
