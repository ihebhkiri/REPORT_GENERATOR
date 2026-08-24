package RHIS.com.RHIS.auth.auth.services;


import RHIS.com.RHIS.auth.auth.models.RefreshTokenEntity;
import RHIS.com.RHIS.auth.auth.repositories.RefreshTokenRepo;
import RHIS.com.RHIS.auth.user.UserRepository;
import RHIS.com.RHIS.core.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepo refreshTokenRepo;
    private final UserRepository userRepository;
   public String generateRefreshToken (UserDetails userDetails) {

        String refreshToken = UUID.randomUUID()
                .toString();
        RefreshTokenEntity refreshTokenEntity = RefreshTokenEntity.builder()
                .token(refreshToken)
                .user(userRepository.findByEmail(userDetails.getUsername())
                        .orElseThrow(() -> new UserNotFoundException("User not found")))
                .creationDate(Instant.now())
                .expiryDate(Instant.now()
                        .plusSeconds(518400))
                .build();
        refreshTokenRepo.save(refreshTokenEntity);
        return refreshToken;
    }

}
