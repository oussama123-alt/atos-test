package com.example.atosTest.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Business rules for user registration, bound from the {@code registration.*} properties.
 *
 * @param allowedCountry ISO 3166-1 alpha-2 code of the only country whose residents may register
 * @param minimumAge     minimum age (in years) required to register
 */
@Validated
@ConfigurationProperties(prefix = "registration")
public record RegistrationProperties(
        @NotBlank String allowedCountry,
        @Positive int minimumAge
) {
}
