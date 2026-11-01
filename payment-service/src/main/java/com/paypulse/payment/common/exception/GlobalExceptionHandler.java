
package com.paypulse.payment.common.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import org.springframework.security.authentication.BadCredentialsException;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MerchantAlreadyExistsException.class)
    public ProblemDetail handleDuplicateMerchant(
            MerchantAlreadyExistsException ex) {

        return problem(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(MerchantNotFoundException.class)
    public ProblemDetail handleMerchantNotFound(
            MerchantNotFoundException ex) {

        return problem(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    public ProblemDetail handlePaymentNotFound(
            PaymentNotFoundException ex) {

        return problem(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ProblemDetail handleIdempotencyConflict(
            IdempotencyConflictException ex) {

        return problem(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(InvalidIdempotencyKeyException.class)
    public ProblemDetail handleInvalidIdempotencyKey(
            InvalidIdempotencyKeyException ex) {

        return problem(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(
            BadCredentialsException ex) {

        return problem(
                HttpStatus.UNAUTHORIZED,
                "Invalid email or password"
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataConflict(
            DataIntegrityViolationException ex) {

        return problem(
                HttpStatus.CONFLICT,
                "The request conflicts with existing data"
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(
            MethodArgumentNotValidException ex) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error ->
                        error.getField() + ": " +
                                error.getDefaultMessage()
                )
                .orElse("Invalid request");

        return problem(HttpStatus.BAD_REQUEST, message);
    }

    private ProblemDetail problem(
            HttpStatus status,
            String message
    ) {

        ProblemDetail detail =
                ProblemDetail.forStatusAndDetail(status, message);

        detail.setTitle(status.getReasonPhrase());

        return detail;
    }
}
