package RHIS.com.RHIS.auth.user.dto;


import java.util.Set;

public record Me(String email , Set<String> roles) {

}
