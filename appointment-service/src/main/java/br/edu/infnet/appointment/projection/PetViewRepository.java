package br.edu.infnet.appointment.projection;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PetViewRepository extends JpaRepository<PetView, Long> {

    List<PetView> findByOwnerId(Long ownerId);
}
