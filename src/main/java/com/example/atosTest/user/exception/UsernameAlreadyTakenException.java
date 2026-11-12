package com.example.atosTest.user.exception;

/**
 * Thrown when trying to register a username that is already used (case-insensitive).
 */
public class UsernameAlreadyTakenException extends RuntimeException {

    public UsernameAlreadyTakenException(String username) {
        super("Username '" + username + "' is already taken");
    }
}
