package br.edu.infnet.petclinic.events.dto;

import java.util.List;

public record OwnerSnapshot(
        Long id,
        String name,
        String email,
        String phone,
        List<Long> petIds
) {}
