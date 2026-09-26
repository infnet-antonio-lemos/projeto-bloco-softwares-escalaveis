package br.edu.infnet.appointment.projection;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "pet_views")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PetView {

    @Id
    private Long id;

    @Column(nullable = false)
    private String name;

    private String species;

    private String breed;

    private Long ownerId;

    private String ownerName;

    private Instant lastEventAt;

    private Instant updatedAt;
}
