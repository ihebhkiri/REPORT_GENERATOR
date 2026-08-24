package RHIS.com.RHIS.auth.auth.repositories;


import RHIS.com.RHIS.auth.auth.models.PasswordResetTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PassResetRepo extends JpaRepository<PasswordResetTokenEntity, Long> {

     Optional<PasswordResetTokenEntity> findByToken (String token);
}
