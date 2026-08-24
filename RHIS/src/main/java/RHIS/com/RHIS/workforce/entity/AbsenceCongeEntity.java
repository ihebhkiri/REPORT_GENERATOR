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
        name = "rhis_absence_conge",
        indexes = {
                @Index(name = "idx_rhis_absence_employee", columnList = "emp_fk_id"),
                @Index(name = "idx_rhis_absence_type_event", columnList = "type_event_fk_id")
        }
)
public class AbsenceCongeEntity extends EntityUuid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "absence_conge_pk_id")
    private Long id;

    @Column(name = "date_debut")
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Column(name = "heure_debut")
    private LocalTime heureDebut;

    @Column(name = "heure_fin")
    private LocalTime heureFin;

    @Column(name = "duree_jour", nullable = false)
    private float dureeJour;

    @Column(name = "periode_horaire", nullable = false)
    private boolean periodeHoraire;

    @Column(name = "status")
    private String status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "emp_fk_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_rhis_absence_employee")
    )
    private EmployeeEntity employee;

    @Column(name = "type_event_fk_id")
    private Long typeEvenementId;

    public AbsenceCongeEntity(
            UUID uuid,
            LocalDate dateDebut,
            LocalDate dateFin,
            LocalTime heureDebut,
            LocalTime heureFin,
            float dureeJour,
            boolean periodeHoraire,
            String status,
            EmployeeEntity employee
    ) {
        super(uuid);
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.heureDebut = heureDebut;
        this.heureFin = heureFin;
        this.dureeJour = dureeJour;
        this.periodeHoraire = periodeHoraire;
        this.status = status;
        this.employee = Objects.requireNonNull(employee, "employee ne doit pas etre null");
    }
}
