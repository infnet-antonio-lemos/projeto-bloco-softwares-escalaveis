package br.edu.infnet.appointment.projection;

/** O que o agendamento precisa saber sobre um pet, independentemente de onde veio. */
public record ResolvedPet(Long id, String name, Long ownerId, String ownerName) {
}
