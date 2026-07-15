package br.edu.infnet.petclinic;

import br.edu.infnet.petclinic.owner.OwnerService;
import br.edu.infnet.petclinic.owner.dto.OwnerRequest;
import br.edu.infnet.petclinic.owner.dto.OwnerResponse;
import br.edu.infnet.petclinic.owner.dto.OwnerRevisionResponse;
import br.edu.infnet.petclinic.pet.PetService;
import br.edu.infnet.petclinic.pet.Species;
import br.edu.infnet.petclinic.pet.dto.PetRequest;
import br.edu.infnet.petclinic.pet.dto.PetResponse;
import br.edu.infnet.petclinic.pet.dto.PetRevisionResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes de integração do histórico (Hibernate Envers).
 *
 * A classe NÃO é @Transactional de propósito: cada chamada de serviço commita
 * em sua própria transação, gerando uma revisão Envers distinta por operação.
 * As asserções usam entidades criadas no próprio teste (o seed de data.sql não
 * possui revisões e não interfere).
 */
@SpringBootTest
class AuditHistoryTest {

    @Autowired
    private OwnerService ownerService;

    @Autowired
    private PetService petService;

    @Test
    void ownerHistoryRecordsInsertUpdateAndDelete() {
        OwnerResponse created = ownerService.create(
                new OwnerRequest("Hank Pym", "hank.history@email.com", "(11) 91111-1111", "Rua A, 1"));
        Long id = created.id();

        ownerService.update(id,
                new OwnerRequest("Hank Pym Jr", "hank.history@email.com", "(11) 92222-2222", "Rua B, 2"));
        ownerService.delete(id);

        List<OwnerRevisionResponse> history = ownerService.findHistory(id);

        assertThat(history).hasSize(3);
        assertThat(history).extracting(OwnerRevisionResponse::revisionType)
                .containsExactly("INSERT", "UPDATE", "DELETE");
        // revisões em ordem crescente
        assertThat(history).extracting(OwnerRevisionResponse::revision).isSorted();
        // estado capturado em cada revisão
        assertThat(history.get(0).state().name()).isEqualTo("Hank Pym");
        assertThat(history.get(1).state().name()).isEqualTo("Hank Pym Jr");
        // store_data_at_delete=true → a revisão de DELETE preserva o último estado
        assertThat(history.get(2).state().name()).isEqualTo("Hank Pym Jr");
    }

    @Test
    void petHistoryRecordsInsertUpdateAndDelete() {
        OwnerResponse owner = ownerService.create(
                new OwnerRequest("Janet Van Dyne", "janet.history@email.com", null, null));

        PetResponse pet = petService.create(
                new PetRequest("Buddy", Species.DOG, "Beagle", LocalDate.of(2021, 6, 1), owner.id()));
        Long petId = pet.id();

        petService.update(petId,
                new PetRequest("Buddy", Species.DOG, "Beagle Mix", LocalDate.of(2021, 6, 1), owner.id()));
        petService.delete(petId);

        List<PetRevisionResponse> history = petService.findHistory(petId);

        assertThat(history).hasSize(3);
        assertThat(history).extracting(PetRevisionResponse::revisionType)
                .containsExactly("INSERT", "UPDATE", "DELETE");
        assertThat(history.get(0).state().breed()).isEqualTo("Beagle");
        assertThat(history.get(1).state().breed()).isEqualTo("Beagle Mix");
        assertThat(history.get(2).state().breed()).isEqualTo("Beagle Mix");
    }

    @Test
    void petHistoryResolvesOwnerSeededViaSql() {
        // O owner de id 1 vem do data.sql (sem revisões Envers). O histórico do Pet
        // deve resolver o owner atual mesmo assim (targetAuditMode = NOT_AUDITED).
        PetResponse pet = petService.create(
                new PetRequest("Seeded", Species.BIRD, "Canary", LocalDate.of(2020, 1, 1), 1L));
        petService.update(pet.id(),
                new PetRequest("Seeded", Species.BIRD, "Canary 2", LocalDate.of(2020, 1, 1), 1L));

        List<PetRevisionResponse> history = petService.findHistory(pet.id());

        // Não deve lançar 500 mesmo com owner sem revisões de auditoria (vindo do data.sql);
        // a FK (ownerId) é sempre preservada no histórico.
        assertThat(history).hasSize(2);
        assertThat(history.get(0).state().ownerId()).isEqualTo(1L);
        // nome do owner resolvido pela camada de serviço (owner do data.sql, sem revisões próprias)
        assertThat(history.get(0).state().ownerName()).isNotBlank();
        assertThat(history.get(1).state().breed()).isEqualTo("Canary 2");
    }

    @Test
    void findHistoryThrowsWhenEntityNeverExisted() {
        assertThatThrownBy(() -> ownerService.findHistory(999_999L))
                .isInstanceOf(NoSuchElementException.class);
    }
}
