package com.example.atosTest.user.exception;

/**
 * Thrown when the country of residence cannot be matched to an ISO 3166-1 country.
 */
public class UnknownCountryException extends RuntimeException {

    public UnknownCountryException(String country) {
        super("Unknown country '" + country + "': use an ISO 3166-1 alpha-2 code (e.g. FR) or an English country name (e.g. France)");
    }
}
