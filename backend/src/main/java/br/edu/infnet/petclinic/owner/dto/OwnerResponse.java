package br.edu.infnet.petclinic.owner.dto;

import br.edu.infnet.petclinic.owner.Owner;

import java.time.Instant;

public record OwnerResponse(
    Long id,
    String name,
    String email,
    String phone,
    String address,
    int petCount,
    Instant createdAt,
    Instant updatedAt
) {
    public static OwnerResponse from(Owner owner) {
        return new OwnerResponse(
            owner.getId(),
            owner.getName(),
            owner.getEmail(),
            owner.getPhone(),
            owner.getAddress(),
            owner.getPets() == null ? 0 : owner.getPets().size(),
            owner.getCreatedAt(),
            owner.getUpdatedAt()
        );
    }
}
