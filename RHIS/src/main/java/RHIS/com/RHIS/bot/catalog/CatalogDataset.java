package RHIS.com.RHIS.bot.catalog;

import java.util.List;

public record CatalogDataset(Long datasetId, String displayName, List<CatalogField> fields,
        String description, List<String> aliases) {
}
