package RHIS.com.RHIS.bot.catalog;

import java.util.List;

public record ReportCatalog(
        List<CatalogDataset> rootDatasets,
        List<CatalogDataset> relatedDatasets,
        List<CatalogRelation> relations
) {
}
