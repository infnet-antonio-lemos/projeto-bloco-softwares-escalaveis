package br.edu.infnet.appointment.projection;

import br.edu.infnet.appointment.client.PetClient;
import br.edu.infnet.appointment.client.dto.PetSummary;
import br.edu.infnet.appointment.config.PetRegistryUnavailableException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PetDirectory {

    private final PetViewRepository repository;
    private final PetClient petClient;

    @Transactional
    public ResolvedPet resolve(Long petId) {
        Optional<PetView> local = repository.findById(petId);

        if (local.isPresent()) {
            PetView view = local.get();
            return new ResolvedPet(view.getId(), view.getName(), view.getOwnerId(), view.getOwnerName());
        }

        log.info("Pet {} ausente da projeção — recorrendo ao cadastro via HTTP (fallback)", petId);
        return fromRegistry(petId);
    }

    private ResolvedPet fromRegistry(Long petId) {
        try {
            PetSummary pet = petClient.getPet(petId);
            cache(pet);
            return new ResolvedPet(pet.id(), pet.name(), pet.ownerId(), pet.ownerName());
        } catch (FeignException.NotFound ex) {
            throw new NoSuchElementException("Pet not found: " + petId);
        } catch (FeignException ex) {
            throw new PetRegistryUnavailableException(
                    "Cadastro de pets indisponível no momento. Tente novamente.", ex);
        }
    }

    private void cache(PetSummary pet) {
        repository.save(PetView.builder()
                .id(pet.id())
                .name(pet.name())
                .ownerId(pet.ownerId())
                .ownerName(pet.ownerName())
                .updatedAt(Instant.now())
                .build());
    }
}
