package com.example.atosTest.user.domain;

import com.example.atosTest.user.domain.User.Gender;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private static User user(String username) {
        return new User(username, LocalDate.of(1990, 3, 21), "FR", "+33612345678", Gender.OTHER, Instant.now());
    }

    @Test
    void persistsAndReloadsAllAttributes() {
        User saved = userRepository.saveAndFlush(user("jdupont"));

        assertThat(saved.getId()).isNotNull();
        assertThat(userRepository.findById(saved.getId())).get()
                .usingRecursiveComparison()
                .isEqualTo(saved);
    }
}
