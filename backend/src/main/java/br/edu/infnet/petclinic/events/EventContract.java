package br.edu.infnet.petclinic.events;


public final class EventContract {

    /** Topic exchange por onde saem todos os eventos de cadastro (pets e tutores). */
    public static final String REGISTRY_EXCHANGE = "petclinic.registry";

    public static final String PET_CREATED = "pet.created";
    public static final String PET_UPDATED = "pet.updated";
    public static final String PET_DELETED = "pet.deleted";
    public static final String OWNER_CREATED = "owner.created";
    public static final String OWNER_UPDATED = "owner.updated";
    public static final String OWNER_DELETED = "owner.deleted";

    /**
     * Versão do esquema do envelope. Viaja em cada mensagem para que um consumidor
     * antigo reconheça — e rejeite explicitamente — um formato que não sabe ler,
     * em vez de interpretá-lo errado em silêncio.
     */
    public static final int SCHEMA_VERSION = 1;

    private EventContract() {
    }
}
