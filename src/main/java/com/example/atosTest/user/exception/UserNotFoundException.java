package com.example.atosTest.user.exception;

/**
 * Thrown when no user exists with the requested id.
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long id) {
        super("No user found with id " + id);
    }
}
