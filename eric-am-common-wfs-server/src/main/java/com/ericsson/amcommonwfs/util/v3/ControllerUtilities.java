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
package com.ericsson.amcommonwfs.util.v3;

import static java.util.regex.Pattern.compile;

import static com.ericsson.amcommonwfs.util.Constants.FILE_NULL_EMPTY_ERROR_MESSAGE;
import static com.ericsson.amcommonwfs.util.Constants.PROCESS_INSTANCE_NULL;
import static com.ericsson.amcommonwfs.util.Constants.VALUES_FILE_INVALID_FILE_NAME_ERROR_MESSAGE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.ERROR_MESSAGE;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RELEASE_NAME;

import java.io.IOException;
import java.io.InputStream;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

import org.camunda.bpm.engine.runtime.ProcessInstanceWithVariables;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.amcommonwfs.exception.InstanceServiceException;
import com.ericsson.amcommonwfs.util.RestPayloadValidationUtils;
import com.ericsson.workflow.orchestration.mgmt.model.v3.ResourceResponseSuccess;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ControllerUtilities {

    private static final String PROCESS_INSTANCE_DETAILS = "Process Instance details :: {}";
    private static final String REGEX_FOR_VALUES_YAML_FILE_NAME =
            "^((?![`~!@#$%^&*()|+\\-=?;:'\"," + "<>\\{\\}\\[\\]\\\\\\/]).)*\\.(yaml|yml)$";
    private static final java.util.regex.Pattern PATTERN_FOR_VALUES_YAML_FILE_NAME = compile(
            REGEX_FOR_VALUES_YAML_FILE_NAME);

    private static final String SELF_LINK = "self";
    private static final String INSTANCE_LINK = "instance";

    private ControllerUtilities() {
    }

    public static ResourceResponseSuccess extractResourceResponse(
            final ProcessInstanceWithVariables processInstanceWithVariables) {
        LOGGER.debug(PROCESS_INSTANCE_DETAILS, processInstanceWithVariables);
        if (processInstanceWithVariables != null) {
            return prepareResourceResponseSuccess(processInstanceWithVariables.getId(),
                    processInstanceWithVariables.getVariables());
        } else {
            throw new InstanceServiceException(PROCESS_INSTANCE_NULL);
        }
    }

    public static ResourceResponseSuccess prepareResourceResponseSuccess(String instanceId, Map<String, Object> variables) {
        if (variables.containsKey(ERROR_MESSAGE)) {
            throw new IllegalArgumentException(
                    (String) variables.get(ERROR_MESSAGE));
        } else {
            ResourceResponseSuccess resourceResponse = new ResourceResponseSuccess();
            resourceResponse.setInstanceId(instanceId);
            resourceResponse.setReleaseName((String) variables.get(RELEASE_NAME));
            return resourceResponse;
        }
    }

    public static void validateValuesFile(final MultipartFile values) {
        final String originalFilename = values.getOriginalFilename();
        LOGGER.info("Validating '{}' values file", originalFilename);
        RestPayloadValidationUtils.validateFileName(originalFilename, PATTERN_FOR_VALUES_YAML_FILE_NAME,
                String.format(FILE_NULL_EMPTY_ERROR_MESSAGE, "Values"), VALUES_FILE_INVALID_FILE_NAME_ERROR_MESSAGE);
        try (InputStream instr1 = RestPayloadValidationUtils.getInputStream(values);
                InputStream instr2 = RestPayloadValidationUtils.getInputStream(values)) {
            RestPayloadValidationUtils.validateFileTypeAsPlainText(instr1);
            RestPayloadValidationUtils.validateYamlCanBeParsed(instr2);
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static ResponseEntity<ResourceResponseSuccess> buildOperationResponse(ResourceResponseSuccess resourceResponseSuccess,
                                                                                  HttpServletRequest httpServletRequest) {
        if (resourceResponseSuccess.getReleaseName() != null) {
            Map<String, String> links = new HashMap<>();
            String selfLink = formatLink(httpServletRequest, resourceResponseSuccess);
            links.put(SELF_LINK, selfLink);
            links.put(INSTANCE_LINK, selfLink + "?instanceId=" + resourceResponseSuccess.getInstanceId());
            resourceResponseSuccess.setLinks(links);
        }
        return new ResponseEntity<>(resourceResponseSuccess, HttpStatus.ACCEPTED);
    }

    public static String formatLink(final HttpServletRequest httpServletRequest,
            final ResourceResponseSuccess resourceResponse) {
        String releaseName = resourceResponse.getReleaseName();
        String url = httpServletRequest.getRequestURL().toString();
        if (url.contains(releaseName)) {
            return url.replaceAll(releaseName + ".*", releaseName);
        }
        return url + "/" + releaseName;
    }

}
