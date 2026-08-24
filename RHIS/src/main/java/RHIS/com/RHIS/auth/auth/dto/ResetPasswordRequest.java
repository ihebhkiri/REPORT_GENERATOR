package RHIS.com.RHIS.auth.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


public record ResetPasswordRequest(
        @NotBlank (message = "Password is required")
        @Size (min = 6, message = "Password should be at least 6 characters")
        String password,

        @NotBlank (message = "Confirm password is required")
        String confirmPassword,

        @NotBlank (message = "Token is required")
        String token) {

}
