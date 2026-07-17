package br.edu.infnet.petclinic.pet;

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
        return PetResponse.from(petRepository.save(pet));
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
        return PetResponse.from(petRepository.save(pet));
    }

    public void delete(Long id) {
        if (!petRepository.existsById(id)) {
            throw new NoSuchElementException("Pet not found: " + id);
        }
        petRepository.deleteById(id);
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
