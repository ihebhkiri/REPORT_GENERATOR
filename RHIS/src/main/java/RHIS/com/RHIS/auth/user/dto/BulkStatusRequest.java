package RHIS.com.RHIS.auth.user.dto;


import java.util.List;

public record BulkStatusRequest(
        List<Long> ids,
        Boolean enabled
) {
}
