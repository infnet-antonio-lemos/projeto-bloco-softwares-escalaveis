package br.edu.infnet.petclinic.pet;

import br.edu.infnet.petclinic.config.JpaAuditingConfig;
import br.edu.infnet.petclinic.owner.Owner;
import br.edu.infnet.petclinic.owner.OwnerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes da camada de persistência do {@link PetRepository} (fatia @DataJpaTest),
 * cobrindo as queries derivadas findByOwnerId / findBySpecies e os metadados de auditoria.
 * O seed de data.sql é desabilitado para asserções determinísticas sobre um schema limpo.
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
@TestPropertySource(properties = "spring.sql.init.mode=never")
class PetRepositoryTest {

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private OwnerRepository ownerRepository;

    private Owner owner;

    @BeforeEach
    void setUp() {
        owner = ownerRepository.save(Owner.builder()
                .name("Frank Castle")
                .email("frank@email.com")
                .build());
    }

    private Pet pet(String name, Species species) {
        return Pet.builder()
                .name(name)
                .species(species)
                .breed("mixed")
                .birthDate(LocalDate.of(2022, 1, 1))
                .owner(owner)
                .build();
    }

    @Test
    void findByOwnerIdReturnsOnlyThatOwnersPets() {
        petRepository.save(pet("Rex", Species.DOG));
        petRepository.save(pet("Mia", Species.CAT));

        Owner other = ownerRepository.save(Owner.builder().name("Other").email("other@email.com").build());
        petRepository.save(Pet.builder().name("Zeca").species(Species.BIRD).owner(other).build());

        assertThat(petRepository.findByOwnerId(owner.getId()))
                .hasSize(2)
                .extracting(Pet::getName)
                .containsExactlyInAnyOrder("Rex", "Mia");
    }

    @Test
    void findBySpeciesFiltersByEnum() {
        petRepository.save(pet("Rex", Species.DOG));
        petRepository.save(pet("Thor", Species.DOG));
        petRepository.save(pet("Mia", Species.CAT));

        assertThat(petRepository.findBySpecies(Species.DOG))
                .extracting(Pet::getName)
                .containsExactlyInAnyOrder("Rex", "Thor");
        assertThat(petRepository.findBySpecies(Species.RABBIT)).isEmpty();
    }

    @Test
    void populatesAuditingMetadataOnPersist() {
        Pet saved = petRepository.saveAndFlush(pet("Rex", Species.DOG));

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
}
