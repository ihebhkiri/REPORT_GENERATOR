package RHIS.com.RHIS.workforce.repository;

import RHIS.com.RHIS.workforce.entity.ShiftEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShiftRepository extends JpaRepository<ShiftEntity, Long> {
}
