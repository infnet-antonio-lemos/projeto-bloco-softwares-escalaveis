package br.edu.infnet.appointment.appointment;

import br.edu.infnet.appointment.config.JpaAuditingConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fatia de persistência do repositório dedicado do microsserviço.
 * {@code spring.sql.init.mode=never} desliga o data.sql para que os testes
 * controlem inteiramente o conteúdo da tabela.
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
@TestPropertySource(properties = "spring.sql.init.mode=never")
class AppointmentRepositoryTest {

    private static final LocalDateTime SLOT = LocalDateTime.of(2030, 5, 20, 9, 0);

    @Autowired
    private AppointmentRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    void seed() {
        persist(1L, 1L, "Rex", SLOT, "Dra. Helena", AppointmentStatus.SCHEDULED);
        persist(2L, 1L, "Mimi", SLOT.plusHours(1), "Dr. Marcos", AppointmentStatus.SCHEDULED);
        persist(3L, 2L, "Thor", SLOT.plusDays(1), "Dra. Helena", AppointmentStatus.COMPLETED);
    }

    @Test
    void findByPetIdReturnsOnlyThatPetsAppointments() {
        List<Appointment> found = repository.findByPetIdOrderByScheduledAtDesc(1L);

        assertThat(found).hasSize(1);
        assertThat(found.getFirst().getPetName()).isEqualTo("Rex");
    }

    @Test
    void findByOwnerIdGroupsAppointmentsOfAllPetsOfTheOwner() {
        List<Appointment> found = repository.findByOwnerIdOrderByScheduledAtDesc(1L);

        assertThat(found).hasSize(2);
        assertThat(found).extracting(Appointment::getPetName).containsExactly("Mimi", "Rex");
    }

    @Test
    void findByStatusFiltersByLifecycleStage() {
        assertThat(repository.findByStatusOrderByScheduledAtDesc(AppointmentStatus.SCHEDULED)).hasSize(2);
        assertThat(repository.findByStatusOrderByScheduledAtDesc(AppointmentStatus.COMPLETED)).hasSize(1);
        assertThat(repository.findByStatusOrderByScheduledAtDesc(AppointmentStatus.NO_SHOW)).isEmpty();
    }

    @Test
    void findAllOrdersByScheduledAtDescending() {
        List<Appointment> found = repository.findAllByOrderByScheduledAtDesc();

        assertThat(found).extracting(Appointment::getPetName).containsExactly("Thor", "Mimi", "Rex");
    }

    @Test
    void existsByVetAndSlotDetectsOnlyActiveAppointments() {
        assertThat(repository.existsByVeterinarianAndScheduledAtAndStatus(
                "Dra. Helena", SLOT, AppointmentStatus.SCHEDULED)).isTrue();

        // Mesmo vet e horário livre
        assertThat(repository.existsByVeterinarianAndScheduledAtAndStatus(
                "Dra. Helena", SLOT.plusHours(3), AppointmentStatus.SCHEDULED)).isFalse();

        // A consulta de Thor existe nesse horário, mas está COMPLETED: não bloqueia a agenda
        assertThat(repository.existsByVeterinarianAndScheduledAtAndStatus(
                "Dra. Helena", SLOT.plusDays(1), AppointmentStatus.SCHEDULED)).isFalse();
    }

    @Test
    void auditingMetadataIsPopulatedOnPersist() {
        Appointment saved = repository.findByPetIdOrderByScheduledAtDesc(1L).getFirst();

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    private void persist(Long petId, Long ownerId, String petName,
                         LocalDateTime scheduledAt, String vet, AppointmentStatus status) {
        entityManager.persistAndFlush(Appointment.builder()
                .petId(petId)
                .ownerId(ownerId)
                .petName(petName)
                .ownerName("Tutor " + ownerId)
                .scheduledAt(scheduledAt)
                .veterinarian(vet)
                .reason("Consulta de rotina")
                .status(status)
                .build());
    }
}
