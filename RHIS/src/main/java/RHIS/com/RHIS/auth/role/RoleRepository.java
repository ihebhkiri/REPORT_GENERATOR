package RHIS.com.RHIS.auth.role;


import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface RoleRepository  extends JpaRepository<RoleEntity, Long> {
        Optional<RoleEntity> findByName(String name);

    List<RoleEntity> findAllByNameIn(Set<String> name);
}
