package RHIS.com.RHIS.workforce.repository;

import RHIS.com.RHIS.workforce.entity.RestaurantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantRepository extends JpaRepository<RestaurantEntity, Long> {
}
