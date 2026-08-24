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
        name = "rhis_pointage",
        indexes = {
                @Index(name = "idx_rhis_pointage_employee", columnList = "id_employee"),
                @Index(name = "idx_rhis_pointage_shift", columnList = "id_shift"),
                @Index(name = "idx_rhis_pointage_restaurant", columnList = "id_restaurant"),
                @Index(name = "idx_rhis_pointage_type", columnList = "type_pointage_fk_id"),
                @Index(name = "idx_rhis_pointage_franchise", columnList = "id_franchise")
        }
)
public class PointageEntity extends EntityUuid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pointage_pk_id")
    private Long id;

    @Column(name = "date_journee")
    private LocalDate dateJournee;

    @Column(name = "heure_debut")
    private LocalTime heureDebut;

    @Column(name = "heure_fin")
    private LocalTime heureFin;

    @Column(name = "temps_pointes", nullable = false)
    private float tempsPointes;

    @Column(name = "is_acheval", nullable = false)
    private boolean aCheval;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_employee",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_rhis_pointage_employee")
    )
    private EmployeeEntity employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_shift",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_rhis_pointage_shift")
    )
    private ShiftEntity shift;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_restaurant",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_rhis_pointage_restaurant")
    )
    private RestaurantEntity restaurant;

    @Column(name = "type_pointage_fk_id")
    private Long typePointageId;

    @Column(name = "id_franchise")
    private Long franchiseId;

    public PointageEntity(
            UUID uuid,
            LocalDate dateJournee,
            LocalTime heureDebut,
            LocalTime heureFin,
            float tempsPointes,
            boolean aCheval,
            EmployeeEntity employee,
            ShiftEntity shift,
            RestaurantEntity restaurant
    ) {
        super(uuid);
        this.dateJournee = dateJournee;
        this.heureDebut = heureDebut;
        this.heureFin = heureFin;
        this.tempsPointes = tempsPointes;
        this.aCheval = aCheval;
        this.employee = Objects.requireNonNull(employee, "employee ne doit pas etre null");
        this.shift = Objects.requireNonNull(shift, "shift ne doit pas etre null");
        this.restaurant = Objects.requireNonNull(restaurant, "restaurant ne doit pas etre null");
    }
}
