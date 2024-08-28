/*
 * COPYRIGHT Ericsson 2024
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 */
package com.ericsson.amcommonwfs.exception.handlers;

import static org.springframework.core.Ordered.LOWEST_PRECEDENCE;

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.ericsson.amcommonwfs.exception.CompleteErrorDescription;
import com.ericsson.amcommonwfs.exception.ErrorMessage;

import lombok.extern.slf4j.Slf4j;

@ControllerAdvice
@Order(LOWEST_PRECEDENCE)
@Slf4j
public class DefaultExceptionHandler {

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<CompleteErrorDescription> handleAll(Throwable throwable) {
        LOGGER.error("Unknown Exception Occured , message={}", throwable.getMessage(), throwable);
        final List<ErrorMessage> errorDetails = new ArrayList<>();
        errorDetails.add(new ErrorMessage(throwable.getMessage()));
        return new ResponseEntity<>(new CompleteErrorDescription(errorDetails), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
