package RHIS.com.RHIS.auth.role;



import RHIS.com.RHIS.auth.role.dto.RoleRequest;
import RHIS.com.RHIS.auth.role.dto.RoleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping ("/api/v1/admin/roles")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    public ResponseEntity<List<RoleResponse>> getAllRoles () {

        return ResponseEntity.ok(roleService.getAllRoles());
    }

    @PostMapping
    public ResponseEntity<RoleEntity> createRole (@RequestBody RoleRequest request) {
        
        String roleName = request.name();
        if (roleName != null && !roleName.startsWith("ROLE_")) {
            roleName = "ROLE_" + roleName.toUpperCase();
        } else if (roleName != null) {
            roleName = roleName.toUpperCase();
        }
        
        return ResponseEntity.ok(roleService.createRole(roleName));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoleEntity> updateRole (@PathVariable Long id, @RequestBody RoleRequest request) {
        String roleName = request.name();
        if (roleName != null && !roleName.startsWith("ROLE_")) {
            roleName = "ROLE_" + roleName.toUpperCase();
        } else if (roleName != null) {
            roleName = roleName.toUpperCase();
        }
        return ResponseEntity.ok(roleService.updateRole(id, roleName));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole (@PathVariable Long id) {
        roleService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }
}

