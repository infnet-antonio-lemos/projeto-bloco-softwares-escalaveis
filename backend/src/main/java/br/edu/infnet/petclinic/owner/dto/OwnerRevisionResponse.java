package br.edu.infnet.petclinic.owner.dto;

import br.edu.infnet.petclinic.owner.Owner;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionType;

import java.time.Instant;

/**
 * Uma revisão do histórico de um {@link Owner}: número/momento da revisão,
 * o tipo de mudança (INSERT/UPDATE/DELETE) e o estado da entidade naquela revisão.
 */
public record OwnerRevisionResponse(
    Long revision,
    String revisionType,
    Instant revisionInstant,
    OwnerResponse state
) {
    public static OwnerRevisionResponse from(Owner entity, DefaultRevisionEntity revisionEntity, RevisionType type) {
        return new OwnerRevisionResponse(
            (long) revisionEntity.getId(),
            describe(type),
            revisionEntity.getRevisionDate().toInstant(),
            OwnerResponse.from(entity)
        );
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
