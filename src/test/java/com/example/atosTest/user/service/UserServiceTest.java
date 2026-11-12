package com.example.atosTest.user.service;

import com.example.atosTest.config.RegistrationProperties;
import com.example.atosTest.user.domain.User.Gender;
import com.example.atosTest.user.domain.User;
import com.example.atosTest.user.domain.UserRepository;
import com.example.atosTest.user.dto.UserRegistrationRequest;
import com.example.atosTest.user.dto.UserResponse;
import com.example.atosTest.user.exception.RegistrationNotAllowedException;
import com.example.atosTest.user.exception.UsernameAlreadyTakenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-15T10:00:00Z");

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(
                userRepository,
                new CountryResolver(),
                new RegistrationProperties("FR", 18),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static UserRegistrationRequest request(String country, LocalDate birthdate) {
        return new UserRegistrationRequest("jdupont", birthdate, country, "+33612345678", Gender.MALE);
    }

    private static User persisted(User user, long id) {
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Nested
    class Register {

        @Test
        void savesAndReturnsAnEligibleUser() {
            when(userRepository.existsByUsernameIgnoreCase("jdupont")).thenReturn(false);
            when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> persisted(inv.getArgument(0), 1L));

            UserResponse response = userService.register(request("France", LocalDate.of(1990, 3, 21)));

            assertThat(response).isEqualTo(new UserResponse(
                    1L, "jdupont", LocalDate.of(1990, 3, 21), "FR", "+33612345678", Gender.MALE, NOW));
        }

        @Test
        void storesTheCountryAsIsoCode() {
            when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> persisted(inv.getArgument(0), 1L));

            userService.register(request("france", LocalDate.of(1990, 3, 21)));

            ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
            verify(userRepository).saveAndFlush(saved.capture());
            assertThat(saved.getValue().getCountryOfResidence()).isEqualTo("FR");
        }

        @Test
        void acceptsOptionalAttributesBeingAbsent() {
            when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> persisted(inv.getArgument(0), 1L));

            UserResponse response = userService.register(
                    new UserRegistrationRequest("jdupont", LocalDate.of(1990, 3, 21), "FR", null, null));

            assertThat(response.phoneNumber()).isNull();
            assertThat(response.gender()).isNull();
        }




        @Test
        void rejectsNonFrenchResidents() {
            assertThatThrownBy(() -> userService.register(request("Germany", LocalDate.of(1990, 3, 21))))
                    .isInstanceOf(RegistrationNotAllowedException.class)
                    .hasMessage("Only residents of France can register");

            verify(userRepository, never()).saveAndFlush(any());
        }


        @Test
        void rejectsAnAlreadyTakenUsername() {
            when(userRepository.existsByUsernameIgnoreCase("jdupont")).thenReturn(true);

            assertThatThrownBy(() -> userService.register(request("FR", LocalDate.of(1990, 3, 21))))
                    .isInstanceOf(UsernameAlreadyTakenException.class)
                    .hasMessage("Username 'jdupont' is already taken");

            verify(userRepository, never()).saveAndFlush(any());
        }

    }

    @Nested
    class GetById {

        @Test
        void returnsTheUser() {
            User user = persisted(new User("jdupont", LocalDate.of(1990, 3, 21), "FR", null, Gender.FEMALE, NOW), 7L);
            when(userRepository.findById(7L)).thenReturn(Optional.of(user));

            UserResponse response = userService.getById(7L);

            assertThat(response.id()).isEqualTo(7L);
            assertThat(response.username()).isEqualTo("jdupont");
            assertThat(response.gender()).isEqualTo(Gender.FEMALE);
        }

    }
}
