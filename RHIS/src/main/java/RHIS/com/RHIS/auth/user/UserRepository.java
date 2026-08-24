package RHIS.com.RHIS.auth.user;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByEmail (String email);
    long countByEnabledTrue();

    long countByEnabledFalse();

    boolean existsByEmail (String email);

    @Query ("SELECT u FROM UserEntity u LEFT JOIN FETCH u.roles WHERE u.email = :email")

    Optional<UserEntity> findByEmailWithRoles (String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM UserEntity u WHERE u.id = :id")
    Optional<UserEntity> findByIdForUpdate(Long id);

    Page<UserEntity> findByEmailContainingIgnoreCaseAndRoles_NameIgnoreCaseAndEnabled (String email, String role,Boolean enabled, Pageable pageable);

    Page<UserEntity> findByEmailContainingIgnoreCase (String email, Pageable pageable);

    Page<UserEntity> findByRoles_NameIgnoreCase (String role, Pageable pageable);

    Page<UserEntity> findByEnabled (Boolean enabled, Pageable pageable);

    Page<UserEntity> findByEmailContainingIgnoreCaseAndRoles_NameIgnoreCase (String email, String email1, Pageable peageable);

    Page<UserEntity> findByEmailContainingIgnoreCaseAndEnabled (String email, Boolean enabled, Pageable pageable);

    Page<UserEntity> findByRoles_NameIgnoreCaseAndEnabled (String roleName, Boolean enabled, Pageable pageable);
}
