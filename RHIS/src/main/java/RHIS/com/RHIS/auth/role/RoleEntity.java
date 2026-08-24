package RHIS.com.RHIS.auth.role;



import jakarta.persistence.*;
import lombok.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor

@Entity
@Getter
@Setter
@Table (name = "roles")
public class RoleEntity {

    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true)
    private String name;

}
