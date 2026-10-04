package by.ares.transaction_validation_service.exception.handler;

import by.ares.transaction_validation_service.exception.FxRateNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(FxRateNotFoundException.class)
    public ProblemDetail handleFxRateNotFoundException(FxRateNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Exchange Rate Not Found");
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }
}
