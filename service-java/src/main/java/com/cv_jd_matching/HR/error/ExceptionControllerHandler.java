package com.cv_jd_matching.HR.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class ExceptionControllerHandler {
    private final Logger LOGGER = LoggerFactory.getLogger(ExceptionControllerHandler.class);

    @ExceptionHandler(WrongWeightsException.class)
    public ResponseEntity<HttpErrorResponse> weightsException(WrongWeightsException exception)
    {
        LOGGER.error(exception.getMessage());
        return createHttpResponse(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(PathException.class)
    public ResponseEntity<HttpErrorResponse> pathException(PathException exception)
    {
        LOGGER.error(exception.getMessage());
        return createHttpResponse(HttpStatus.BAD_REQUEST, exception.getMessage());
    }


    @ExceptionHandler(InputException.class)
    public ResponseEntity<HttpErrorResponse> idsException(PathException exception)
    {
        LOGGER.error(exception.getMessage());
        return createHttpResponse(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    private ResponseEntity<HttpErrorResponse> createHttpResponse(HttpStatus httpStatus, String message){
        HttpErrorResponse httpErrorResponse = HttpErrorResponse.builder()
                .timeStamp(LocalDateTime.now())
                .httpStatusCode(httpStatus.value())
                .reason(httpStatus.getReasonPhrase().toUpperCase())
                .message(message)
                .build();
        return new ResponseEntity<>(httpErrorResponse, httpStatus);
    }

}
