package com.usth.hospital.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record BulkDeleteRequest(

        @NotEmpty
        @Size(max = 1000, message = "At most 1000 patients can be deleted at once")
        List<@NotBlank(message = "Patient ID is required") String> ids
) {
}
