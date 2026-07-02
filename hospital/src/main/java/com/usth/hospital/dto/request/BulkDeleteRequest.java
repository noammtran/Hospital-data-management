package com.usth.hospital.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record BulkDeleteRequest(

        @NotEmpty
        List<String> ids
) {
}
