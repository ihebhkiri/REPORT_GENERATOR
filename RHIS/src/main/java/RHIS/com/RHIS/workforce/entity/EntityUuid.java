package RHIS.com.RHIS.workforce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;

import java.util.Objects;
import java.util.UUID;

@Getter
@MappedSuperclass
public abstract class EntityUuid {

    @Column(name = "uuid", nullable = false, unique = true, updatable = false)
    private UUID uuid;

    protected EntityUuid() {
        this.uuid = UUID.randomUUID();
    }

    protected EntityUuid(UUID uuid) {
        this.uuid = Objects.requireNonNull(uuid, "uuid ne doit pas etre null");
    }

    @PrePersist
    private void initializeUuid() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
    }
}
