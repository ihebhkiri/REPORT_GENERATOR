package RHIS.com.RHIS.dataset.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Setter
@Table(
        name = "datasets",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_datasets_source_name",
                columnNames = "source_name"
        )
)
@Getter
@NoArgsConstructor()
public class DataSetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(length = 1000)
    private String description = "";

    @Column(length = 2000)
    private String aliases = "";

    @Column(name = "source_name", nullable = false)
    private String sourceName;

    @Column(nullable = false)
    private boolean active;
    @Column(nullable = false)
    private boolean displayMain;
    @Column(nullable = false)
    private boolean displayRelated;


    @OneToMany(mappedBy = "dataset", fetch = FetchType.LAZY)
    private Set<DataSetField> dataSetFieldSet = new HashSet<>();

    public DataSetEntity(String displayName, String sourceName) {
        this.displayName = displayName;
        this.sourceName = sourceName;
        this.active = true;
        this.displayMain = true;
    }
}
