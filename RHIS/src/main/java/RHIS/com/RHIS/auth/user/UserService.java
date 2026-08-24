package RHIS.com.RHIS.auth.user;



import RHIS.com.RHIS.auth.role.RoleEntity;
import RHIS.com.RHIS.auth.role.RoleRepository;
import RHIS.com.RHIS.auth.user.dto.CreateUserRequest;
import RHIS.com.RHIS.auth.user.dto.UpdateUserRequest;
import RHIS.com.RHIS.auth.user.dto.UserDataResponse;
import RHIS.com.RHIS.auth.user.dto.UserStatsResponse;
import RHIS.com.RHIS.core.exception.RoleNotFoundException;
import RHIS.com.RHIS.core.exception.UserAlreadyExistsException;
import RHIS.com.RHIS.core.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final PasswordEncoder passwordEncoder;

    public void createUser (CreateUserRequest request) throws RoleNotFoundException {

        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("Email already exists");
        }
        Set<String> baseRole = request.roleNames()
                .stream()
                .map(name -> "ROLE_" + name.toUpperCase())
                .collect(Collectors.toSet());
        List<RoleEntity> roles = roleRepository.findAllByNameIn((baseRole));
        if (roles.size() != request.roleNames()
                .size()) {
            throw new RoleNotFoundException("One or more roles not found");
        }
        UserEntity userEntity = new UserEntity();
        userEntity.setEmail(request.email());
        userEntity.setPassword(passwordEncoder.encode(request.password()));
        userEntity.getRoles()
                .addAll(roles);
        userRepository.save(userEntity);
    }

    @Transactional (readOnly = true)
    public Page<UserDataResponse> getAllUsers (Pageable pageable, String email, String role, Boolean status) {


        String roleName = (role != null && !role.isEmpty())
                ? "ROLE_" + role
                : null;

        if (email != null && !email.isEmpty() && roleName != null && status != null) {
            return userRepository
                    .findByEmailContainingIgnoreCaseAndRoles_NameIgnoreCaseAndEnabled(email, roleName, status, pageable)
                    .map(this::mapToResponse);
        }

        if (email != null && !email.isEmpty() && roleName != null) {
            return userRepository
                    .findByEmailContainingIgnoreCaseAndRoles_NameIgnoreCase(email, roleName, pageable)
                    .map(this::mapToResponse);
        }

        if (email != null && !email.isEmpty() && status != null) {
            return userRepository
                    .findByEmailContainingIgnoreCaseAndEnabled(email, status, pageable)
                    .map(this::mapToResponse);
        }

        if (roleName != null && status != null) {
            return userRepository
                    .findByRoles_NameIgnoreCaseAndEnabled(roleName, status, pageable)
                    .map(this::mapToResponse);
        }

        if (email != null && !email.isEmpty()) {
            return userRepository
                    .findByEmailContainingIgnoreCase(email, pageable)
                    .map(this::mapToResponse);
        }

        if (roleName != null) {
            return userRepository
                    .findByRoles_NameIgnoreCase(roleName, pageable)
                    .map(this::mapToResponse);
        }

        if (status != null) {
            return userRepository
                    .findByEnabled(status, pageable)
                    .map(this::mapToResponse);
        }

        return userRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    @Transactional (readOnly = true)
    public UserDataResponse getUserById (Long id) {

        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return mapToResponse(user);
    }

    public void updateUser (Long id, UpdateUserRequest request)  {

        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (request.email() != null) {
            user.setEmail(request.email());
        }

        if (request.roleNames() != null) {
            Set<String> baseRole = request.roleNames()
                    .stream()
                    .map(name -> "ROLE_" + name.toUpperCase())
                    .collect(Collectors.toSet());
            List<RoleEntity> roles = roleRepository.findAllByNameIn((baseRole));
            if (roles.size() != request.roleNames()
                    .size()) {
                throw new RoleNotFoundException("One or more roles not found");
            }
            user.getRoles()
                    .clear();
            user.getRoles()
                    .addAll(roles);
        }


        userRepository.save(user);
    }

    public void deleteUser (Long id) {

        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException("User not found");
        }
        userRepository.deleteById(id);
    }


    public void changeUserStatus (Long id, Boolean enabled) {

        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        user.setEnabled(enabled);
        userRepository.save(user);

    }

    public void bulkDeleteUsers (List<Long> ids) {

        userRepository.deleteAllById(ids);
    }

    public void bulkChangeStatus (List<Long> ids, Boolean enabled) {

        List<UserEntity> users = userRepository.findAllById(ids);
        users.forEach(user -> user.setEnabled(enabled));
        userRepository.saveAll(users);
    }


    private UserDataResponse mapToResponse (UserEntity user) {

        return
                new UserDataResponse(
                        user.getId(),
                        user.getEmail(),
                        user.getRoles()
                                .stream()
                                .map(RoleEntity::getName)
                                .collect(Collectors.toSet()),
                        user.getCreatedAt(),
                        user.getUpdatedAt(),
                        user.isEnabled()
                );
    }
    public UserStatsResponse getUserStats () {

        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByEnabledTrue();
        long inactiveUsers = userRepository.countByEnabledFalse();

        return new UserStatsResponse(totalUsers, activeUsers, inactiveUsers);
    }
}
