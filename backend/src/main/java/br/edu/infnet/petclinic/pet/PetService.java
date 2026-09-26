package br.edu.infnet.petclinic.pet;

import br.edu.infnet.petclinic.events.DomainEventPublisher;
import br.edu.infnet.petclinic.events.EventContract;
import br.edu.infnet.petclinic.events.dto.PetSnapshot;
import br.edu.infnet.petclinic.owner.Owner;
import br.edu.infnet.petclinic.owner.OwnerRepository;
import br.edu.infnet.petclinic.pet.dto.PetRequest;
import br.edu.infnet.petclinic.pet.dto.PetResponse;
import br.edu.infnet.petclinic.pet.dto.PetRevisionResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional
public class PetService {

    private final PetRepository petRepository;
    private final OwnerRepository ownerRepository;
    private final DomainEventPublisher events;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<PetResponse> findAll(Long ownerId, Species species) {
        if (ownerId != null) {
            return petRepository.findByOwnerId(ownerId).stream().map(PetResponse::from).toList();
        }
        if (species != null) {
            return petRepository.findBySpecies(species).stream().map(PetResponse::from).toList();
        }
        return petRepository.findAll().stream().map(PetResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PetResponse findById(Long id) {
        return petRepository.findById(id)
                .map(PetResponse::from)
                .orElseThrow(() -> new NoSuchElementException("Pet not found: " + id));
    }

    public PetResponse create(PetRequest request) {
        Owner owner = ownerRepository.findById(request.ownerId())
                .orElseThrow(() -> new NoSuchElementException("Owner not found: " + request.ownerId()));
        Pet pet = Pet.builder()
                .name(request.name())
                .species(request.species())
                .breed(request.breed())
                .birthDate(request.birthDate())
                .owner(owner)
                .build();
        Pet saved = petRepository.save(pet);
        events.publish(EventContract.PET_CREATED, "pet", saved.getId(), snapshotOf(saved));
        return PetResponse.from(saved);
    }

    public PetResponse update(Long id, PetRequest request) {
        Pet pet = petRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Pet not found: " + id));
        Owner owner = ownerRepository.findById(request.ownerId())
                .orElseThrow(() -> new NoSuchElementException("Owner not found: " + request.ownerId()));
        pet.setName(request.name());
        pet.setSpecies(request.species());
        pet.setBreed(request.breed());
        pet.setBirthDate(request.birthDate());
        pet.setOwner(owner);
        Pet saved = petRepository.save(pet);
        events.publish(EventContract.PET_UPDATED, "pet", saved.getId(), snapshotOf(saved));
        return PetResponse.from(saved);
    }

    public void delete(Long id) {
        Pet pet = petRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Pet not found: " + id));
        // O snapshot é capturado antes do delete: o evento precisa dizer *o que* foi
        // apagado, e depois do delete a informação não existe mais para ser lida.
        PetSnapshot snapshot = snapshotOf(pet);
        petRepository.delete(pet);
        events.publish(EventContract.PET_DELETED, "pet", id, snapshot);
    }

    private PetSnapshot snapshotOf(Pet pet) {
        Owner owner = pet.getOwner();
        return new PetSnapshot(
                pet.getId(),
                pet.getName(),
                pet.getSpecies() == null ? null : pet.getSpecies().name(),
                pet.getBreed(),
                owner == null ? null : owner.getId(),
                owner == null ? null : owner.getName());
    }

    /**
     * Retorna o histórico completo de revisões do Pet (INSERT/UPDATE/DELETE),
     * do mais antigo para o mais recente, usando o Hibernate Envers.
     */
    @Transactional(readOnly = true)
    public List<PetRevisionResponse> findHistory(Long id) {
        AuditReader reader = AuditReaderFactory.get(entityManager);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = reader.createQuery()
                .forRevisionsOfEntity(Pet.class, false, true)
                .add(AuditEntity.id().eq(id))
                .addOrder(AuditEntity.revisionNumber().asc())
                .getResultList();

        if (rows.isEmpty()) {
            throw new NoSuchElementException("No history found for Pet: " + id);
        }

        return rows.stream()
                .map(row -> {
                    Pet pet = (Pet) row[0];
                    String ownerName = pet.getOwner() == null ? null
                            : ownerRepository.findById(pet.getOwner().getId())
                                    .map(Owner::getName).orElse(null);
                    return PetRevisionResponse.from(
                            pet,
                            (DefaultRevisionEntity) row[1],
                            (RevisionType) row[2],
                            ownerName);
                })
                .toList();
    }
}
