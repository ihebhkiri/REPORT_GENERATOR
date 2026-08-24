package RHIS.com.RHIS.auth.auth.repositories;


import RHIS.com.RHIS.auth.auth.models.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepo extends JpaRepository<RefreshTokenEntity,Long> {
    Optional<RefreshTokenEntity> findByToken (String request);
}
