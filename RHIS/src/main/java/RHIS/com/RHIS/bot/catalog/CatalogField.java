package RHIS.com.RHIS.bot.catalog;

import java.util.List;

public record CatalogField(Long fieldId, String displayName, String type, List<String> operators,
        String description, List<String> aliases) {
}
