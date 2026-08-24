package RHIS.com.RHIS;

import RHIS.com.RHIS.core.exception.RessourceNotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public abstract class GenericCrudServiceImpl<T, ID> implements GenericCrudService<T, ID> {
    private final JpaRepository<T, ID> repository;
    private final String resourceName;

    public GenericCrudServiceImpl(JpaRepository<T, ID> repository, String resourceName) {
        this.repository = repository;
        this.resourceName = resourceName;
    }

    @Override
    public T create(T entity) {
        return repository.save(entity);
    }

    @Override
    public T update(ID id, T entity) {
        if (!repository.existsById(id)) {
            throw new RessourceNotFoundException(resourceName, id);
        }
        return repository.save(entity);
    }

    @Override
    public Optional<T> findById(ID id) {
        return repository.findById(id);
    }

    @Override
    public List<T> findAll() {
        return repository.findAll();
    }

    @Override
    public void deleteById(ID id) {
        repository.deleteById(id);
    }

    @Override
    public boolean existsById(ID id) {
        return repository.existsById(id);
    }

}
