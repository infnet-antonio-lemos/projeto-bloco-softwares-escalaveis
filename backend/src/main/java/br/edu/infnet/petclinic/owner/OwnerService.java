package br.edu.infnet.petclinic.owner;

import br.edu.infnet.petclinic.events.DomainEventPublisher;
import br.edu.infnet.petclinic.events.EventContract;
import br.edu.infnet.petclinic.events.dto.OwnerSnapshot;
import br.edu.infnet.petclinic.pet.Pet;
import br.edu.infnet.petclinic.owner.dto.OwnerRequest;
import br.edu.infnet.petclinic.owner.dto.OwnerResponse;
import br.edu.infnet.petclinic.owner.dto.OwnerRevisionResponse;
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
public class OwnerService {

    private final OwnerRepository repository;
    private final DomainEventPublisher events;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<OwnerResponse> findAll() {
        return repository.findAll().stream()
                .map(OwnerResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OwnerResponse findById(Long id) {
        return repository.findById(id)
                .map(OwnerResponse::from)
                .orElseThrow(() -> new NoSuchElementException("Owner not found: " + id));
    }

    public OwnerResponse create(OwnerRequest request) {
        Owner owner = Owner.builder()
                .name(request.name())
                .email(request.email())
                .phone(request.phone())
                .address(request.address())
                .build();
        Owner saved = repository.save(owner);
        events.publish(EventContract.OWNER_CREATED, "owner", saved.getId(), snapshotOf(saved));
        return OwnerResponse.from(saved);
    }

    public OwnerResponse update(Long id, OwnerRequest request) {
        Owner owner = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Owner not found: " + id));
        owner.setName(request.name());
        owner.setEmail(request.email());
        owner.setPhone(request.phone());
        owner.setAddress(request.address());
        Owner saved = repository.save(owner);
        events.publish(EventContract.OWNER_UPDATED, "owner", saved.getId(), snapshotOf(saved));
        return OwnerResponse.from(saved);
    }

    public void delete(Long id) {
        Owner owner = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Owner not found: " + id));
        OwnerSnapshot snapshot = snapshotOf(owner);
        repository.delete(owner);
        events.publish(EventContract.OWNER_DELETED, "owner", id, snapshot);
    }

    private OwnerSnapshot snapshotOf(Owner owner) {
        List<Long> petIds = owner.getPets() == null
                ? List.of()
                : owner.getPets().stream().map(Pet::getId).toList();
        return new OwnerSnapshot(
                owner.getId(), owner.getName(), owner.getEmail(), owner.getPhone(), petIds);
    }

    /**
     * Retorna o histórico completo de revisões do Owner (INSERT/UPDATE/DELETE),
     * do mais antigo para o mais recente, usando o Hibernate Envers.
     */
    @Transactional(readOnly = true)
    public List<OwnerRevisionResponse> findHistory(Long id) {
        AuditReader reader = AuditReaderFactory.get(entityManager);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = reader.createQuery()
                .forRevisionsOfEntity(Owner.class, false, true)
                .add(AuditEntity.id().eq(id))
                .addOrder(AuditEntity.revisionNumber().asc())
                .getResultList();

        if (rows.isEmpty()) {
            throw new NoSuchElementException("No history found for Owner: " + id);
        }

        return rows.stream()
                .map(row -> OwnerRevisionResponse.from(
                        (Owner) row[0],
                        (DefaultRevisionEntity) row[1],
                        (RevisionType) row[2]))
                .toList();
    }
}
