package com.smartpay.payment.exception;

import com.smartpay.payment.domain.exception.PaymentNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PaymentNotFoundException.class)
    public ProblemDetail handleInvalidRefreshToken(PaymentNotFoundException e, HttpServletRequest httpServletRequest) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problemDetail.setTitle("Payment Not Found");
        problemDetail.setInstance(URI.create(httpServletRequest.getRequestURI()));
        return problemDetail;
    }
}
