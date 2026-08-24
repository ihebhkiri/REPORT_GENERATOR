package RHIS.com.RHIS.auth.user;




import RHIS.com.RHIS.auth.user.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping ("/api/v1/admin/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    @PreAuthorize ("hasRole('ADMIN')")
    public ResponseEntity<Void> createUser (
            @Valid @RequestBody CreateUserRequest request) {

        userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .build();
    }

    @GetMapping
    @PreAuthorize ("hasRole('ADMIN')")
    public Page<UserDataResponse> getAllUsers (Pageable pageable,
                                               @RequestParam (required = false) String email,
                                               @RequestParam (required = false, defaultValue = "") String role,
                                               @RequestParam (required = false) Boolean status
    ) {

        return userService.getAllUsers(pageable, email, role, status);
    }

    @GetMapping ("/{id}")
    @PreAuthorize ("hasRole('ADMIN')")
    public ResponseEntity<UserDataResponse> getUserById (@PathVariable Long id) {

        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping ("/{id}")
    @PreAuthorize ("hasRole('ADMIN')")
    public ResponseEntity<Void> updateUser (
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {

        userService.updateUser(id, request);
        return ResponseEntity.noContent()
                .build();
    }

    @DeleteMapping ("/{id}")
    @PreAuthorize ("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser (@PathVariable Long id) {

        userService.deleteUser(id);
        return ResponseEntity.noContent()
                .build();
    }

    @PatchMapping ("/{id}/status")
    @PreAuthorize ("hasRole('ADMIN')")
    public ResponseEntity<Void> changeUserStatus (@PathVariable Long id, @RequestBody Map<String, Boolean> body) {

        Boolean enabled = body.get("enabled");
        userService.changeUserStatus(id, enabled);
        return ResponseEntity.noContent()
                .build();
    }

    @DeleteMapping("/bulk")
    @PreAuthorize ("hasRole('ADMIN')")
    public ResponseEntity<Void> bulkDeleteUsers(@RequestBody List<Long> ids) {
        userService.bulkDeleteUsers(ids);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/bulk/status")
    @PreAuthorize ("hasRole('ADMIN')")
    public ResponseEntity<Void> bulkChangeStatus(@RequestBody BulkStatusRequest request) {
        userService.bulkChangeStatus(request.ids(), request.enabled());
        return ResponseEntity.noContent().build();
    }


    @GetMapping ("/stats")
    @PreAuthorize ("hasRole('ADMIN')")
    public ResponseEntity<UserStatsResponse> getUserStats () {

        UserStatsResponse stats = userService.getUserStats();
        return ResponseEntity.ok(stats);
    }


}

