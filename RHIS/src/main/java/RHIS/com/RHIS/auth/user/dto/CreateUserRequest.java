package RHIS.com.RHIS.auth.user.dto;


import java.util.Set;

public record CreateUserRequest(String email,

                                String password,
                                Set<String> roleNames) {


}
