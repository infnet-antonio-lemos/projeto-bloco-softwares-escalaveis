package br.edu.infnet.petclinic.pet.dto;

import br.edu.infnet.petclinic.owner.Owner;
import br.edu.infnet.petclinic.pet.Pet;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionType;

import java.time.Instant;

/**
 * Uma revisão do histórico de um {@link Pet}: número/momento da revisão,
 * o tipo de mudança (INSERT/UPDATE/DELETE) e o estado da entidade naquela revisão.
 */
public record PetRevisionResponse(
    Long revision,
    String revisionType,
    Instant revisionInstant,
    PetResponse state
) {
    /**
     * @param ownerName nome do owner já resolvido pela camada de serviço (pode ser {@code null}
     *                  se o owner não existir mais). Evita inicializar o proxy do Owner
     *                  reconstruído pelo Envers, que nem sempre é resolúvel em revisões antigas.
     */
    public static PetRevisionResponse from(Pet entity, DefaultRevisionEntity revisionEntity,
                                           RevisionType type, String ownerName) {
        return new PetRevisionResponse(
            (long) revisionEntity.getId(),
            describe(type),
            revisionEntity.getRevisionDate().toInstant(),
            historyState(entity, ownerName)
        );
    }

    /** Monta o estado do Pet para o histórico; o {@code ownerId} vem da FK (sem inicializar o proxy). */
    private static PetResponse historyState(Pet pet, String ownerName) {
        Owner owner = pet.getOwner();
        Long ownerId = owner == null ? null : owner.getId();
        return new PetResponse(
            pet.getId(), pet.getName(), pet.getSpecies(), pet.getBreed(), pet.getBirthDate(),
            ownerId, ownerName, pet.getCreatedAt(), pet.getUpdatedAt());
    }

    /** Traduz o tipo de revisão do Envers (ADD/MOD/DEL) para rótulos legíveis. */
    static String describe(RevisionType type) {
        return switch (type) {
            case ADD -> "INSERT";
            case MOD -> "UPDATE";
            case DEL -> "DELETE";
        };
    }
}
