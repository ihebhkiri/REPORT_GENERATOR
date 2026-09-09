package RHIS.com.RHIS.dataset.entity;


import RHIS.com.RHIS.dataset.model.DataSetFieldType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "dataset_fields",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_dataset_fields_dataset_source",
                columnNames = {"dataset_id", "source_name"}
        )
)
@Getter
@Setter
@NoArgsConstructor()
public class DataSetField {

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
    private boolean visible = true;

    @Column(name = "field_position", nullable = false)
    private int position;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 32)
    private DataSetFieldType dataType = DataSetFieldType.UNSUPPORTED;

    @Column(nullable = false)
    private boolean nullable;

    @Column(name = "primary_key", nullable = false)
    private boolean primaryKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dataset_id", nullable = false)
    private DataSetEntity dataset;

    public DataSetField(
            String displayName,
            String sourceName,
            int position,
            DataSetEntity dataset
    ) {
        this.displayName = displayName;
        this.sourceName = sourceName;
        this.position = position;
        this.dataset = dataset;
        this.active = true;
        this.visible = true;
    }


}
