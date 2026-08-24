package RHIS.com.RHIS.auth.user.dto;


import java.util.Set;

public record UpdateUserRequest(

        String email,

        Set<String> roleNames) {
}

