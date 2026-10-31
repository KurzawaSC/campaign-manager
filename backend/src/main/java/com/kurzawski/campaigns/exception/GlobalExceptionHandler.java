package com.kurzawski.campaigns.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String VALIDATION_FAILED = "Validation failed";

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> fieldErrors.merge(
                error.getField(),
                error.getDefaultMessage(),
                (first, second) -> first + "; " + second));
        return ResponseEntity.badRequest().body(validationProblem(fieldErrors));
    }

    @ExceptionHandler(InvalidReferenceException.class)
    public ProblemDetail handleInvalidReference(InvalidReferenceException ex) {
        return validationProblem(Map.of(ex.getField(), ex.getMessage()));
    }

    @ExceptionHandler(CampaignNotFoundException.class)
    public ProblemDetail handleCampaignNotFound(CampaignNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail handleProductNotFound(ProductNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DuplicateProductNameException.class)
    public ProblemDetail handleDuplicateProductName(DuplicateProductNameException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setProperty("fieldErrors", Map.of("name", ex.getMessage()));
        return problem;
    }

    @ExceptionHandler(ProductInUseException.class)
    public ProblemDetail handleProductInUse(ProductInUseException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ProblemDetail handleInsufficientFunds(InsufficientFundsException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, DataIntegrityViolationException.class})
    public ProblemDetail handleConcurrentChange(Exception ex) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "The data was changed by another request. Refresh and try again.");
    }

    private ProblemDetail validationProblem(Map<String, String> fieldErrors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, VALIDATION_FAILED);
        problem.setProperty("fieldErrors", fieldErrors);
        return problem;
    }
}
