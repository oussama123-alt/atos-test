package com.example.atosTest.user.service;

import com.example.atosTest.config.RegistrationProperties;
import com.example.atosTest.user.domain.User;
import com.example.atosTest.user.domain.UserRepository;
import com.example.atosTest.user.dto.UserRegistrationRequest;
import com.example.atosTest.user.dto.UserResponse;
import com.example.atosTest.user.exception.RegistrationNotAllowedException;
import com.example.atosTest.user.exception.UnknownCountryException;
import com.example.atosTest.user.exception.UserNotFoundException;
import com.example.atosTest.user.exception.UsernameAlreadyTakenException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final CountryResolver countryResolver;
    private final RegistrationProperties registrationProperties;
    private final Clock clock;

    public UserService(UserRepository userRepository,
                       CountryResolver countryResolver,
                       RegistrationProperties registrationProperties,
                       Clock clock) {
        this.userRepository = userRepository;
        this.countryResolver = countryResolver;
        this.registrationProperties = registrationProperties;
        this.clock = clock;
    }

    /**
     * Registers a new user after checking the eligibility rules.
     *
     * @throws UnknownCountryException          if the country of residence is not a known country
     * @throws RegistrationNotAllowedException  if the user is not a resident of the allowed country or is under age
     * @throws UsernameAlreadyTakenException    if the username is already used
     */
    @Transactional
    public UserResponse register(UserRegistrationRequest request) {
        String countryCode = countryResolver.resolve(request.countryOfResidence())
                .orElseThrow(() -> new UnknownCountryException(request.countryOfResidence()));

        checkCountryIsAllowed(countryCode);
        checkIsAdult(request.birthdate());

        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new UsernameAlreadyTakenException(request.username());
        }

        User user = new User(
                request.username(),
                request.birthdate(),
                countryCode,
                request.phoneNumber(),
                request.gender(),
                Instant.now(clock));

        try {
            return UserResponse.from(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException e) {
            // Concurrent registration of the same username slipped past the existence check.
            throw new UsernameAlreadyTakenException(request.username());
        }
    }

    /**
     * @throws UserNotFoundException if no user exists with this id
     */
    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return userRepository.findById(id)
                .map(UserResponse::from)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    private void checkCountryIsAllowed(String countryCode) {
        String allowedCountry = registrationProperties.allowedCountry();
        if (!allowedCountry.equalsIgnoreCase(countryCode)) {
            String countryName = new Locale("", allowedCountry).getDisplayCountry(Locale.ENGLISH);
            throw new RegistrationNotAllowedException("Only residents of " + countryName + " can register");
        }
    }

    private void checkIsAdult(LocalDate birthdate) {
        int minimumAge = registrationProperties.minimumAge();
        LocalDate today = LocalDate.now(clock);
        if (birthdate.plusYears(minimumAge).isAfter(today)) {
            throw new RegistrationNotAllowedException("User must be at least " + minimumAge + " years old to register");
        }
    }
}
