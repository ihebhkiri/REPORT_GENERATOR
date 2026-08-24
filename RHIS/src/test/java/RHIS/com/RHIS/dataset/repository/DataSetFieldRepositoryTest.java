package RHIS.com.RHIS.dataset.repository;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import static org.assertj.core.api.Assertions.assertThat;

class DataSetFieldRepositoryTest {

    @Test
    void visibleFieldExtractionDependsOnlyOnDatasetAndFieldAvailability() throws Exception {
        Query query = DataSetFieldRepository.class
                .getMethod("findVisibleFieldsByDatasetId", Long.class)
                .getAnnotation(Query.class);

        assertThat(query).isNotNull();
        assertThat(query.value())
                .contains("field.dataset.active = true")
                .contains("field.active = true")
                .contains("field.visible = true")
                .doesNotContain("displayMain")
                .doesNotContain("displayRelated");
    }
}
