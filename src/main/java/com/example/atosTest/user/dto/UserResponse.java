package com.example.atosTest.user.dto;

import com.example.atosTest.user.domain.User;
import com.example.atosTest.user.domain.User.Gender;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Public representation of a registered user.
 */
public record UserResponse(
        Long id,
        String username,
        LocalDate birthdate,
        String countryOfResidence,
        String phoneNumber,
        Gender gender,
        Instant createdAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getBirthdate(),
                user.getCountryOfResidence(),
                user.getPhoneNumber(),
                user.getGender(),
                user.getCreatedAt());
    }
}
