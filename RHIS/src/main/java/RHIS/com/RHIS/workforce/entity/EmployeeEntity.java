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
        name = "rhis_employee",
        indexes = @Index(name = "idx_rhis_employee_restau", columnList = "restau_fk_id")
)
public class EmployeeEntity extends EntityUuid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "emp_pk_id")
    private Long id;

    @Column(name = "matricule")
    private String matricule;

    @Column(name = "nom")
    private String nom;

    @Column(name = "prenom")
    private String prenom;

    @Column(name = "date_entree")
    private LocalDate dateEntree;

    @Column(name = "date_sortie")
    private LocalDate dateSortie;

    @Column(name = "statut", nullable = false)
    private boolean statut;

    @Column(name = "hebdo_courant", nullable = false)
    private float hebdoCourant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "restau_fk_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_rhis_employee_restaurant")
    )
    private RestaurantEntity restaurant;

    public EmployeeEntity(
            UUID uuid,
            String matricule,
            String nom,
            String prenom,
            LocalDate dateEntree,
            LocalDate dateSortie,
            boolean statut,
            float hebdoCourant,
            RestaurantEntity restaurant
    ) {
        super(uuid);
        this.matricule = matricule;
        this.nom = nom;
        this.prenom = prenom;
        this.dateEntree = dateEntree;
        this.dateSortie = dateSortie;
        this.statut = statut;
        this.hebdoCourant = hebdoCourant;
        this.restaurant = Objects.requireNonNull(restaurant, "restaurant ne doit pas etre null");
    }
}
