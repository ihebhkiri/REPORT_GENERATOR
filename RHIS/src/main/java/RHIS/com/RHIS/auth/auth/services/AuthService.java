package RHIS.com.RHIS.auth.auth.services;

import RHIS.com.RHIS.auth.UserPrincipal;
import RHIS.com.RHIS.auth.auth.dto.LoginRequest;
import RHIS.com.RHIS.auth.auth.dto.LoginResponse;
import RHIS.com.RHIS.auth.auth.dto.RefreshTokenResponse;
import RHIS.com.RHIS.auth.user.dto.Me;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Map;

public interface AuthService {
     LoginResponse login(LoginRequest loginRequest) ;
     RefreshTokenResponse refreshToken(String request);
    Me getCurrentUser(UserPrincipal principal) ;



}
