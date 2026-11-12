package com.example.atosTest.user.exception;

/**
 * Thrown when a well-formed registration request does not satisfy the eligibility
 * rules (only adult French residents may register).
 */
public class RegistrationNotAllowedException extends RuntimeException {

    public RegistrationNotAllowedException(String reason) {
        super(reason);
    }
}
