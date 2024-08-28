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

import com.ericsson.am.shared.vnfd.service.exception.CryptoException;
import com.ericsson.am.shared.vnfd.service.exception.ServiceUnavailableException;
import com.ericsson.amcommonwfs.util.Constants;
import com.ericsson.amcommonwfs.utils.repository.FileStorageException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.ProcessEngineException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.ericsson.amcommonwfs.exception.CompleteErrorDescription;
import com.ericsson.amcommonwfs.exception.ErrorMessage;
import com.ericsson.amcommonwfs.exception.InstanceServiceException;
import com.ericsson.amcommonwfs.exception.InvalidRequestParametersException;
import com.ericsson.amcommonwfs.exception.KubeConfigValidationException;
import com.ericsson.amcommonwfs.exception.KubectlAPIException;
import com.ericsson.amcommonwfs.exception.NotFoundException;
import com.ericsson.amcommonwfs.exception.ParameterErrorMessage;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

@ControllerAdvice
@Order(HIGHEST_PRECEDENCE)
@Slf4j
public class ApplicationExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ProcessEngineException.class)
    public ResponseEntity<CompleteErrorDescription> handleProcessEngineException(final ProcessEngineException pee, // NOSONAR
                                                                                 final WebRequest request) {
        LOGGER.error("Process Engine Exception Occurred , {}", pee.getMessage());
        final List<ErrorMessage> errorDetails = new ArrayList<>();
        errorDetails.add(new ErrorMessage(pee.getMessage()));
        return new ResponseEntity<>(new CompleteErrorDescription(errorDetails), HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(InstanceServiceException.class)
    public ResponseEntity<CompleteErrorDescription> handleInstanceServiceException(
            final InstanceServiceException ex, final WebRequest request) { // NOSONAR
        LOGGER.error("Instance Service Exception Occurred , {}", ex.getMessage());
        final List<ErrorMessage> errorDetails = new ArrayList<>();
        errorDetails.add(new ErrorMessage(ex.getMessage()));
        if (ex.getMessage() != null && ex.getMessage().equals(Constants.INSTANCE_DETAILS_NOT_FOUND)) {
            return new ResponseEntity<>(new CompleteErrorDescription(errorDetails), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(new CompleteErrorDescription(errorDetails), HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<CompleteErrorDescription> handleNotFoundException(final NotFoundException ex) { // NOSONAR
        LOGGER.error("NotFoundException Occurred , {}", ex.getMessage());
        final List<ErrorMessage> errorDetails = new ArrayList<>();
        errorDetails.add(new ErrorMessage(ex.getMessage()));
        return new ResponseEntity<>(new CompleteErrorDescription(errorDetails), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CompleteErrorDescription> handleIllegalArgumentException(final IllegalArgumentException ex) { // NOSONAR
        LOGGER.error("IllegalArgumentException Occurred , {}", ex.getMessage());
        final List<ErrorMessage> errorDetails = new ArrayList<>();
        errorDetails.add(new ErrorMessage(ex.getMessage()));
        return new ResponseEntity<>(new CompleteErrorDescription(errorDetails), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(KubectlAPIException.class)
    public ResponseEntity<CompleteErrorDescription> handleKubectlAPIException(
            final KubectlAPIException ex) { // NOSONAR
        LOGGER.error("KubectlAPIException , {}", ex.getMessage());
        final List<ErrorMessage> errorDetails = new ArrayList<>();
        errorDetails.add(new ErrorMessage(ex.getMessage()));
        return new ResponseEntity<>(new CompleteErrorDescription(errorDetails), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<CompleteErrorDescription> handleConstraintViolationException(
        final ConstraintViolationException cve) { // NOSONAR
        LOGGER.error("ConstraintViolationException Occurred, {}", cve.getMessage());
        Set<ConstraintViolation<?>> violations = cve.getConstraintViolations();
        List<Object> error = new ArrayList<>();
        if (!CollectionUtils.isEmpty(violations)) {
            error = violations.stream().map(violation -> ParameterErrorMessage.fromString(violation.getMessage()))
                .distinct().collect(toList());
        }
        return new ResponseEntity<>(new CompleteErrorDescription(error), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(KubeConfigValidationException.class)
    public ResponseEntity<CompleteErrorDescription> handleKubeConfigValidationException(
        KubeConfigValidationException kcve) { // NOSONAR
        LOGGER.error("KubeConfigValidationException occurred, {}", kcve.getMessage());
        final List<ErrorMessage> errorDetails = new ArrayList<>();
        errorDetails.add(new ErrorMessage(kcve.getMessage()));
        return new ResponseEntity<>(new CompleteErrorDescription(errorDetails), HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestPart(MissingServletRequestPartException ex, HttpHeaders headers,
                                                                     HttpStatusCode status, WebRequest request) {
        Map<String, String> errorDetails = new HashMap<>();
        errorDetails.put("parameterName", ex.getRequestPartName());
        errorDetails.put("message", ex.getMessage());
        List<Object> error = new ArrayList<>();
        error.add(errorDetails);
        return new ResponseEntity<>(new CompleteErrorDescription(error), HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex, HttpHeaders headers,
                                                                          HttpStatusCode status, WebRequest request) {
        LOGGER.error("Method argument not valid, exception occurred , {}", ex.getMessage());
        Map<String, String> errorDetails = new HashMap<>();
        errorDetails.put("parameterName", ex.getParameterName());
        errorDetails.put("message", ex.getMessage());
        List<Object> error = new ArrayList<>();
        error.add(errorDetails);
        return new ResponseEntity<>(new CompleteErrorDescription(error), HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        LOGGER.error("Method argument not valid, exception occurred , {}", ex.getMessage());
        List<String> errorMessage = new ArrayList<>();
        final BindingResult bindingResult = ex.getBindingResult();
        if (bindingResult.hasErrors()) {
            List<ObjectError> fieldErrorList = bindingResult.getAllErrors();
            for (ObjectError fieldError : fieldErrorList) {
                String defaultMessage = fieldError.getDefaultMessage();
                errorMessage.add(defaultMessage);
            }
        }
        List<Object> error = new ArrayList<>();
        if (errorMessage.isEmpty()) {
            error.add(new ErrorMessage(ex.getMessage()));
        } else {
            for (String message : errorMessage) {
                error.add(ParameterErrorMessage.fromString(message));
            }
        }
        LOGGER.error("Error details , {}", error);
        return new ResponseEntity<>(new CompleteErrorDescription(error), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<CompleteErrorDescription> handleFileStorageException(FileStorageException fse) {
        LOGGER.error("FileStorageException occurred, {}", fse.getMessage());
        final List<ErrorMessage> errorDetails = new ArrayList<>();
        errorDetails.add(new ErrorMessage(fse.getMessage()));
        return new ResponseEntity<>(new CompleteErrorDescription(errorDetails), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(InvalidRequestParametersException.class)
    public ResponseEntity<CompleteErrorDescription> handleInvalidRequestParametersException(InvalidRequestParametersException ex) {
        CompleteErrorDescription errorDetails = new CompleteErrorDescription(ex.getErrorDetails());
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(CryptoException.class)
    public ResponseEntity<CompleteErrorDescription> handleCryptoException(CryptoException ex) {
        LOGGER.error("CryptoException Occurred , {}", ex.getMessage());
        final List<ErrorMessage> errorDetails = new ArrayList<>();
        errorDetails.add(new ErrorMessage(ex.getMessage()));
        return new ResponseEntity<>(new CompleteErrorDescription(errorDetails), HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<CompleteErrorDescription> handleServiceUnavailableException(ServiceUnavailableException ex) {
        LOGGER.error("ServiceUnavailableException Occurred , {}", ex.getMessage());
        final List<ErrorMessage> errorDetails = new ArrayList<>();
        errorDetails.add(new ErrorMessage(ex.getMessage()));
        return new ResponseEntity<>(new CompleteErrorDescription(errorDetails), HttpStatus.SERVICE_UNAVAILABLE);
    }
}
