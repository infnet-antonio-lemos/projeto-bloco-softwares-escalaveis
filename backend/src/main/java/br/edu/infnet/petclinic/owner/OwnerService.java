package br.edu.infnet.petclinic.owner;

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
        return OwnerResponse.from(repository.save(owner));
    }

    public OwnerResponse update(Long id, OwnerRequest request) {
        Owner owner = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Owner not found: " + id));
        owner.setName(request.name());
        owner.setEmail(request.email());
        owner.setPhone(request.phone());
        owner.setAddress(request.address());
        return OwnerResponse.from(repository.save(owner));
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NoSuchElementException("Owner not found: " + id);
        }
        repository.deleteById(id);
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
