package RHIS.com.RHIS.workforce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "rhis_restaurant")
public class RestaurantEntity extends EntityUuid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "restau_pk_id")
    private Long id;

    @Column(name = "libelle")
    private String libelle;

    @Column(name = "matricule")
    private String matricule;

    @Column(name = "adresse")
    private String adresse;

    @Column(name = "code_pointeuse")
    private String codePointeuse;

    @Column(name = "periode_restaurant")
    private String periodeRestaurant;

    public RestaurantEntity(
            UUID uuid,
            String libelle,
            String matricule,
            String adresse,
            String codePointeuse,
            String periodeRestaurant
    ) {
        super(uuid);
        this.libelle = libelle;
        this.matricule = matricule;
        this.adresse = adresse;
        this.codePointeuse = codePointeuse;
        this.periodeRestaurant = periodeRestaurant;
    }
}
