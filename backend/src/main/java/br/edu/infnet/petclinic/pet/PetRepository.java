package br.edu.infnet.petclinic.pet;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PetRepository extends JpaRepository<Pet, Long> {
    List<Pet> findByOwnerId(Long ownerId);
    List<Pet> findBySpecies(Species species);
}
