package com.example.atosTest.user.web;

import com.example.atosTest.user.exception.RegistrationNotAllowedException;
import com.example.atosTest.user.exception.UnknownCountryException;
import com.example.atosTest.user.exception.UserNotFoundException;
import com.example.atosTest.user.exception.UsernameAlreadyTakenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Map;
import java.util.TreeMap;

/**
 *  400 - malformed or invalid input (unreadable JSON, constraint violations, unknown country)</li>
 * 404 - unknown user
 *  409 - username already taken
 *  422 - (not adult / not French resident)
 *  500 - unexpected error
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    static final String ERRORS_PROPERTY = "errors";

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(UserNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "User not found", ex.getMessage());
    }

    @ExceptionHandler(UsernameAlreadyTakenException.class)
    public ProblemDetail handleUsernameAlreadyTaken(UsernameAlreadyTakenException ex) {
        return problem(HttpStatus.CONFLICT, "Username already taken", ex.getMessage());
    }

    @ExceptionHandler(UnknownCountryException.class)
    public ProblemDetail handleUnknownCountry(UnknownCountryException ex) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Validation failed", ex.getMessage());
        problem.setProperty(ERRORS_PROPERTY, Map.of("countryOfResidence", ex.getMessage()));
        return problem;
    }

    @ExceptionHandler(RegistrationNotAllowedException.class)
    public ProblemDetail handleRegistrationNotAllowed(RegistrationNotAllowedException ex) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, "Registration not allowed", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", "An unexpected error occurred");
    }

    /** Invalid {@code @Valid @RequestBody}. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errors = new TreeMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.merge(fieldError.getField(), messageOf(fieldError), (first, second) -> first + "; " + second);
        }
        return validationFailure(errors);
    }

    /** Invalid path variables / request parameters (e.g. {@code @Positive} id). */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
                                                                            HttpHeaders headers,
                                                                            HttpStatusCode status,
                                                                            WebRequest request) {
        Map<String, String> errors = new TreeMap<>();
        ex.getParameterValidationResults().forEach(result -> {
            String parameter = result.getMethodParameter().getParameterName();
            result.getResolvableErrors().forEach(error ->
                    errors.merge(parameter, messageOf(error), (first, second) -> first + "; " + second));
        });
        return validationFailure(errors);
    }

    /** Wrong type in path variable / request parameter (e.g. {@code /api/users/abc}). */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex,
                                                        HttpHeaders headers,
                                                        HttpStatusCode status,
                                                        WebRequest request) {
        String property = ex.getPropertyName() != null ? ex.getPropertyName() : "parameter";
        String requiredType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "the expected type";
        String message = "'" + ex.getValue() + "' is not a valid " + requiredType;
        return validationFailure(Map.of(property, message));
    }

    /** Malformed JSON, wrong date format, unknown enum value... */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Malformed request",
                "Request body is missing or malformed. Expected JSON with 'birthdate' formatted as yyyy-MM-dd "
                        + "and 'gender' one of MALE, FEMALE, OTHER");
        return ResponseEntity.badRequest().body(problem);
    }

    private static ResponseEntity<Object> validationFailure(Map<String, String> errors) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Validation failed", "One or more fields are invalid");
        problem.setProperty(ERRORS_PROPERTY, errors);
        return ResponseEntity.badRequest().body(problem);
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }

    private static String messageOf(MessageSourceResolvable error) {
        return error.getDefaultMessage() != null ? error.getDefaultMessage() : "is invalid";
    }
}
