package com.smartpay.merchant.exception;

import com.smartpay.merchant.domain.exception.EmailAlreadyExistsException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericError(Exception e, HttpServletRequest request) {
        log.error(e.getMessage(), e);
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problemDetail.setTitle("An unexpected error occurred");
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        return problemDetail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationError(MethodArgumentNotValidException e, HttpServletRequest httpServletRequest) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Request data not valid");
        String errorDetail = e.getBindingResult().getFieldErrors().stream()
                        .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                                .collect(Collectors.joining("; "));
        problemDetail.setDetail(errorDetail);
        problemDetail.setInstance(URI.create(httpServletRequest.getRequestURI()));
        return problemDetail;
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ProblemDetail handleExistingEmailConflict(EmailAlreadyExistsException e, HttpServletRequest httpServletRequest) {
        log.warn(e.getMessage(), e);
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problemDetail.setTitle("Email conflict");
        problemDetail.setDetail("The email used for registration is already taken");
        problemDetail.setInstance(URI.create(httpServletRequest.getRequestURI()));
        return problemDetail;
    }
}
