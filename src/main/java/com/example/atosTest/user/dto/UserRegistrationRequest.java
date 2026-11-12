package com.example.atosTest.user.dto;

import com.example.atosTest.user.domain.User.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

/**
 *
 * @param username           letters,
 * @param birthdate          ISO-8601 date (yyyy-MM-dd), in the past
 * @param countryOfResidence ISO 3166-1 alpha-2 code ("FR") or English country name ("France")
 * @param phoneNumber        optional, 6 to 15 digits with an optional leading '+'
 * @param gender             optional, one of MALE, FEMALE, OTHER
 */
public record UserRegistrationRequest(

        @NotBlank(message = "username is required")
        String username,

        @NotNull(message = "birthdate is required")
        @Past(message = "birthdate must be in the past")
        LocalDate birthdate,

        @NotBlank(message = "countryOfResidence is required")
        String countryOfResidence,

        @Pattern(regexp = "^\\+?[0-9]{6,15}$",
                message = "phoneNumber must contain 6 to 15 digits, optionally prefixed with '+'")
        String phoneNumber,

        Gender gender
) {
}
