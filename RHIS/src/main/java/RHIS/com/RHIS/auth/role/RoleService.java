package RHIS.com.RHIS.auth.role;



import RHIS.com.RHIS.auth.role.dto.RoleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;

    public RoleEntity createRole (String name) {

        if (roleRepository.findByName(name)
                .isPresent()) {
            throw new IllegalArgumentException("Role already exists");
        }
        RoleEntity role = new RoleEntity();
        role.setName(name);
        return roleRepository.save(role);
    }

    public RoleEntity getRole (Long id) {

        return roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    }

    public RoleEntity updateRole (Long id, String newName) {
        RoleEntity role = getRole(id);
        if (roleRepository.findById(id).isPresent()) {
            throw new IllegalArgumentException("Role name already exists");
        }
        role.setName(newName);
        return roleRepository.save(role);
    }


    public void deleteRole (Long id) {

        if (!roleRepository.existsById(id)) {
            throw new IllegalArgumentException("Role not found");
        }
        roleRepository.deleteById(id);
    }

    public List<RoleResponse> getAllRoles () {


        return roleRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private RoleResponse mapToResponse (RoleEntity roleEntity) {

        return new RoleResponse(roleEntity.getId(), roleEntity.getName());
    }
}