package RHIS.com.RHIS.auth.auth.services;


import RHIS.com.RHIS.auth.CustomUserDetailService;
import RHIS.com.RHIS.auth.JwtService;
import RHIS.com.RHIS.auth.UserPrincipal;
import RHIS.com.RHIS.auth.auth.dto.LoginRequest;
import RHIS.com.RHIS.auth.auth.dto.LoginResponse;
import RHIS.com.RHIS.auth.auth.dto.RefreshTokenResponse;
import RHIS.com.RHIS.auth.auth.repositories.RefreshTokenRepo;
import RHIS.com.RHIS.auth.user.UserRepository;
import RHIS.com.RHIS.auth.user.dto.Me;
import RHIS.com.RHIS.core.exception.InvalidRefreshTokenException;
import RHIS.com.RHIS.core.exception.RefreshTokenExpiredException;
import RHIS.com.RHIS.core.exception.RefreshTokenNotFoundException;
import RHIS.com.RHIS.core.exception.UserNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;


@Service
@RequiredArgsConstructor

public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;

    private final AuthenticationManager authenticationManager;

    private final JwtService jwtService;


    private final RefreshTokenRepo refreshTokenRepo;

    private final CustomUserDetailService customUserDetailService;

    private final RefreshTokenService refreshTokenService;

    @Override
    public LoginResponse login(LoginRequest loginRequest) {

        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password()));
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        var claims = createUserRoles(userDetails);
        String accessToken = jwtService.generateToken(claims, userDetails);
        String refreshToken = refreshTokenService.generateRefreshToken(userDetails);
        return new LoginResponse(accessToken, refreshToken);
    }


    @Override
    public RefreshTokenResponse refreshToken(String request) {

        var refreshToken = refreshTokenRepo.findByToken(request)
                .orElseThrow(() -> new RefreshTokenNotFoundException("Invalid refresh token"));

        if (refreshToken.getExpiryDate()
                .isBefore(Instant.now())) {
            refreshToken.setValid(false);
            refreshTokenRepo.save(refreshToken);

            throw new RefreshTokenExpiredException("Refresh token expired");
        }
        if (!refreshToken.isValid()) {
            throw new InvalidRefreshTokenException("Invalid refresh token");
        }
        refreshToken.setValid(false);
        refreshTokenRepo.save(refreshToken);
        String userEmail = refreshToken.getUser()
                .getEmail();
        UserDetails userDetails = customUserDetailService.loadUserByUsername(userEmail);
        var claims = createUserRoles(userDetails);
        String accessToken = jwtService.generateToken(claims, userDetails);
        String newRefreshToken = refreshTokenService.generateRefreshToken(userDetails);
        return new RefreshTokenResponse(newRefreshToken, accessToken);

    }


    @Override
    public Me getCurrentUser(UserPrincipal principal) {
        var user = principal.getUser();
        return new Me(user.getEmail(), user.getRoles()
                .stream()
                .map(role -> role.getName())
                .collect(java.util.stream.Collectors.toSet()));

    }

    /// HELPERS
    private Map<String, Object> createUserRoles(UserDetails userDetails) {

        var user = userRepository.findByEmailWithRoles(userDetails.getUsername())
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        Map<String, Object> claims = new HashMap<>();
        var roles = user.getRoles()
                .stream()
                .map(role -> role.getName())
                .toList();
        claims.put("roles", roles);
        return claims;

    }
}
