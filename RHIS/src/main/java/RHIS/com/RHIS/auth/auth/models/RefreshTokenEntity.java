package RHIS.com.RHIS.auth.auth.models;



import RHIS.com.RHIS.auth.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table (name = "refresh_tokens")
public class RefreshTokenEntity {

    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;

    @Column (unique = true, nullable = false)
    private String token;

    @Column (nullable = false)
    private Instant expiryDate;

    @Column (nullable = false)
    private Instant creationDate;

    @Column (nullable = false, name = "is_valid")
    @Builder.Default
    private boolean isValid =true;

    @ManyToOne
    @JoinColumn (name = "user_id", nullable = false)
    private UserEntity user;

}


