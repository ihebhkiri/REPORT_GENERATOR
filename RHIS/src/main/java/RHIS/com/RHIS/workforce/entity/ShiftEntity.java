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
        name = "rhis_shift",
        indexes = {
                @Index(name = "idx_rhis_shift_employee", columnList = "employee_fk_id"),
                @Index(name = "idx_rhis_shift_restaurant", columnList = "restaurant_fk_id"),
                @Index(name = "idx_rhis_shift_poste", columnList = "post_trav_fk_id"),
                @Index(name = "idx_rhis_shift_franchise", columnList = "id_franchise")
        }
)
public class ShiftEntity extends EntityUuid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shift_pk_id")
    private Long id;

    @Column(name = "date_journee")
    private LocalDate dateJournee;

    @Column(name = "heure_debut")
    private LocalTime heureDebut;

    @Column(name = "heure_fin")
    private LocalTime heureFin;

    @Column(name = "total_heure", nullable = false)
    private long totalHeure;

    @Column(name = "from_planning_manager", nullable = false)
    private boolean fromPlanningManager;

    @Column(name = "from_planning_leader", nullable = false)
    private boolean fromPlanningLeader;

    @Column(name = "create_from_reference", nullable = false)
    private boolean createFromReference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "employee_fk_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_rhis_shift_employee")
    )
    private EmployeeEntity employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "restaurant_fk_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_rhis_shift_restaurant")
    )
    private RestaurantEntity restaurant;

    @Column(name = "post_trav_fk_id")
    private Long posteTravailId;

    @Column(name = "id_franchise")
    private Long franchiseId;

    public ShiftEntity(
            UUID uuid,
            LocalDate dateJournee,
            LocalTime heureDebut,
            LocalTime heureFin,
            long totalHeure,
            boolean fromPlanningManager,
            boolean fromPlanningLeader,
            boolean createFromReference,
            EmployeeEntity employee,
            RestaurantEntity restaurant
    ) {
        super(uuid);
        this.dateJournee = dateJournee;
        this.heureDebut = heureDebut;
        this.heureFin = heureFin;
        this.totalHeure = totalHeure;
        this.fromPlanningManager = fromPlanningManager;
        this.fromPlanningLeader = fromPlanningLeader;
        this.createFromReference = createFromReference;
        this.employee = Objects.requireNonNull(employee, "employee ne doit pas etre null");
        this.restaurant = Objects.requireNonNull(restaurant, "restaurant ne doit pas etre null");
    }
}
