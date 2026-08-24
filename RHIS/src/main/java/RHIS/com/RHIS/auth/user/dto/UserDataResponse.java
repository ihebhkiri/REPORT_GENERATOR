package RHIS.com.RHIS.auth.user.dto;


import java.time.Instant;
import java.util.Set;

public record UserDataResponse(
        Long id,

        String email,

        Set<String> roles,

        Instant createdAt,

        Instant updatedAt,

        boolean isEnabled) {

}
