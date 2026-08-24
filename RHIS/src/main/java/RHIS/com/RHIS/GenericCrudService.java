package RHIS.com.RHIS;

import java.util.List;
import java.util.Optional;

public interface GenericCrudService<T, ID> {
    T create(T entity);

    T update(ID id, T entity);

    Optional<T> findById(ID id);

    List<T> findAll();

    void deleteById(ID id);

    boolean existsById(ID id);

}
