package RHIS.com.RHIS.workforce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "rhis_detail_evenement",
        indexes = {
                @Index(name = "idx_rhis_detail_absence", columnList = "absence_conge_fk_id"),
                @Index(name = "idx_rhis_detail_restaurant", columnList = "id_restaurant"),
                @Index(name = "idx_rhis_detail_franchise", columnList = "id_franchise")
        }
)
public class DetailEvenementEntity extends EntityUuid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "detail_event_pk_id")
    private Long id;

    @Column(name = "date_event")
    private LocalDate dateEvent;

    @Column(name = "nb_heure", nullable = false)
    private float nombreHeures;

    @Column(name = "repartition_heure")
    private Float repartitionHeure;

    @Column(name = "heure_debut")
    private LocalTime heureDebut;

    @Column(name = "heure_fin")
    private LocalTime heureFin;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "absence_conge_fk_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_rhis_detail_absence")
    )
    private AbsenceCongeEntity absenceConge;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_restaurant",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_rhis_detail_restaurant")
    )
    private RestaurantEntity restaurant;

    @Column(name = "id_franchise")
    private Long franchiseId;

    public DetailEvenementEntity(
            UUID uuid,
            LocalDate dateEvent,
            float nombreHeures,
            Float repartitionHeure,
            LocalTime heureDebut,
            LocalTime heureFin,
            AbsenceCongeEntity absenceConge,
            RestaurantEntity restaurant
    ) {
        super(uuid);
        this.dateEvent = dateEvent;
        this.nombreHeures = nombreHeures;
        this.repartitionHeure = repartitionHeure;
        this.heureDebut = heureDebut;
        this.heureFin = heureFin;
        this.absenceConge = Objects.requireNonNull(absenceConge, "absenceConge ne doit pas etre null");
        this.restaurant = Objects.requireNonNull(restaurant, "restaurant ne doit pas etre null");
    }
}
