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
import java.util.Objects;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "rhis_contrat",
        indexes = {
                @Index(name = "idx_rhis_contrat_employee", columnList = "emp_fk_id"),
                @Index(name = "idx_rhis_contrat_restaurant", columnList = "id_restaurant"),
                @Index(name = "idx_rhis_contrat_franchise", columnList = "id_franchise"),
                @Index(name = "idx_rhis_contrat_type", columnList = "type_cont_fk_id"),
                @Index(name = "idx_rhis_contrat_groupe", columnList = "grp_trv_fk_id")
        }
)
public class ContratEntity extends EntityUuid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cont_pk_id")
    private Long id;

    @Column(name = "hebdo", nullable = false)
    private float hebdo;

    @Column(name = "tx_horaire", nullable = false)
    private float tauxHoraire;

    @Column(name = "salaire", nullable = false)
    private float salaire;

    @Column(name = "date_effective")
    private LocalDate dateEffective;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Column(name = "actif", nullable = false)
    private boolean actif;

    @Column(name = "temps_partiel", nullable = false)
    private boolean tempsPartiel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "emp_fk_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_rhis_contrat_employee")
    )
    private EmployeeEntity employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_restaurant",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_rhis_contrat_restaurant")
    )
    private RestaurantEntity restaurant;

    @Column(name = "id_franchise")
    private Long franchiseId;

    @Column(name = "type_cont_fk_id")
    private Long typeContratId;

    @Column(name = "grp_trv_fk_id")
    private Long groupeTravailId;

    public ContratEntity(
            UUID uuid,
            float hebdo,
            float tauxHoraire,
            float salaire,
            LocalDate dateEffective,
            LocalDate dateFin,
            boolean actif,
            boolean tempsPartiel,
            EmployeeEntity employee,
            RestaurantEntity restaurant
    ) {
        super(uuid);
        this.hebdo = hebdo;
        this.tauxHoraire = tauxHoraire;
        this.salaire = salaire;
        this.dateEffective = dateEffective;
        this.dateFin = dateFin;
        this.actif = actif;
        this.tempsPartiel = tempsPartiel;
        this.employee = Objects.requireNonNull(employee, "employee ne doit pas etre null");
        this.restaurant = Objects.requireNonNull(restaurant, "restaurant ne doit pas etre null");
    }
}
