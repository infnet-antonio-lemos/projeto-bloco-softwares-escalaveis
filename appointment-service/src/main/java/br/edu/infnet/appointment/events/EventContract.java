package br.edu.infnet.appointment.events;

public final class EventContract {

    // --- Consumido: eventos de cadastro publicados pelo petclinic-backend ---
    public static final String REGISTRY_EXCHANGE = "petclinic.registry";

    public static final String PET_EVENTS_QUEUE = "scheduling.pet-events";
    public static final String OWNER_EVENTS_QUEUE = "scheduling.owner-events";

    public static final String PET_ROUTING_PATTERN = "pet.*";
    public static final String OWNER_ROUTING_PATTERN = "owner.*";

    public static final String PET_CREATED = "pet.created";
    public static final String PET_UPDATED = "pet.updated";
    public static final String PET_DELETED = "pet.deleted";
    public static final String OWNER_UPDATED = "owner.updated";
    public static final String OWNER_DELETED = "owner.deleted";

    // --- Produzido: eventos do ciclo de vida da consulta ---
    public static final String SCHEDULING_EXCHANGE = "petclinic.scheduling";

    public static final String APPOINTMENT_SCHEDULED = "appointment.scheduled";
    public static final String APPOINTMENT_RESCHEDULED = "appointment.rescheduled";
    public static final String APPOINTMENT_UPDATED = "appointment.updated";
    public static final String APPOINTMENT_CANCELLED = "appointment.cancelled";
    public static final String APPOINTMENT_COMPLETED = "appointment.completed";
    public static final String APPOINTMENT_NO_SHOW = "appointment.no-show";
    public static final String APPOINTMENT_DELETED = "appointment.deleted";

    public static final String APPOINTMENT_ROUTING_PATTERN = "appointment.*";

    public static final int SCHEMA_VERSION = 1;

    private EventContract() {
    }
}
