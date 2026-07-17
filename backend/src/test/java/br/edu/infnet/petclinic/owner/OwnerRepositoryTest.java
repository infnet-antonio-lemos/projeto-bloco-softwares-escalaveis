package br.edu.infnet.petclinic.owner;

import br.edu.infnet.petclinic.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes da camada de persistência do {@link OwnerRepository} (fatia @DataJpaTest).
 * Importa {@link JpaAuditingConfig} para que os metadados de auditoria
 * (createdAt/updatedAt) sejam preenchidos. O seed de data.sql é desabilitado
 * para asserções determinísticas sobre um schema limpo.
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
@TestPropertySource(properties = "spring.sql.init.mode=never")
class OwnerRepositoryTest {

    @Autowired
    private OwnerRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void savesAndFindsOwnerById() {
        Owner saved = repository.save(Owner.builder()
                .name("Diana Prince")
                .email("diana@email.com")
                .phone("(11) 90000-0000")
                .address("Rua Nova, 1")
                .build());

        assertThat(saved.getId()).isNotNull();
        assertThat(repository.findById(saved.getId())).isPresent();
    }

    @Test
    void populatesAuditingMetadataOnPersist() {
        Owner saved = repository.saveAndFlush(Owner.builder()
                .name("Eve Adams")
                .email("eve@email.com")
                .build());

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void enforcesUniqueEmail() {
        repository.saveAndFlush(Owner.builder().name("A").email("dup@email.com").build());

        assertThatThrownBy(() -> {
            repository.saveAndFlush(Owner.builder().name("B").email("dup@email.com").build());
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
