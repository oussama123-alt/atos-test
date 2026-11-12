package com.example.atosTest.user.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CountryResolverTest {

    private final CountryResolver resolver = new CountryResolver();

    @ParameterizedTest
    @ValueSource(strings = {"FR", "fr", " Fr ", "France", "FRANCE", "france"})
    void resolvesFranceFromCodeOrNameIgnoringCaseAndSpaces(String input) {
        assertThat(resolver.resolve(input)).contains("FR");
    }
}
