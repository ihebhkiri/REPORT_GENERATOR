package RHIS.com.RHIS.workforce.repository;

import RHIS.com.RHIS.workforce.entity.EmployeeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<EmployeeEntity, Long> {
}
