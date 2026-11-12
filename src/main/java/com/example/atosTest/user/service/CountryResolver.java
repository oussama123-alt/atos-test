package com.example.atosTest.user.service;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves user input (ISO 3166-1 alpha-2 code or English country name, case-insensitive)
 * to an ISO 3166-1 alpha-2 country code.
 */
@Component
public class CountryResolver {

    private final Map<String, String> codesByLookupKey;

    public CountryResolver() {
        Map<String, String> lookup = new HashMap<>();
        for (String code : Locale.getISOCountries()) {
            lookup.put(normalize(code), code);
            lookup.put(normalize(new Locale("", code).getDisplayCountry(Locale.ENGLISH)), code);
        }
        this.codesByLookupKey = Collections.unmodifiableMap(lookup);
    }

    /**
     * @param country a country code or name, e.g. {@code "fr"}, {@code "FR"}, {@code "France"}
     * @return the matching ISO 3166-1 alpha-2 code, or empty if the country is unknown
     */
    public Optional<String> resolve(String country) {
        if (country == null || country.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(codesByLookupKey.get(normalize(country)));
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
