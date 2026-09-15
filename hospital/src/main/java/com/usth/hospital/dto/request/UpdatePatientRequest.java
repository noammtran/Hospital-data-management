package com.usth.hospital.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdatePatientRequest(

        @NotBlank
        @Size(max = 100)
        String name,

        @PastOrPresent
        LocalDate birthDate,

        @Min(1) @Max(3)
        Integer gender,

        @Size(max = 255)
        String address,

        @Size(max = 20)
        String phoneNo,

        @Email
        @Size(max = 50)
        String email,

        @Pattern(regexp = "^(?:[0-9]{12})?$", message = "Identity card number must contain exactly 12 digits")
        String identityNo,

        @Min(1) @Max(8)
        Integer bloodType,

        @Size(max = 500)
        String allergies,

        @Size(max = 1000)
        String medicalHistory,

        @Size(max = 20)
        String insuranceNo,

        LocalDate insuranceExpiry,

        @Size(max = 100)
        String emergencyContactName,

        @Size(max = 20)
        String emergencyContactPhone,

        @Size(max = 50)
        String emergencyContactRelation,

        @Size(max = 1000)
        String note
) {
}
