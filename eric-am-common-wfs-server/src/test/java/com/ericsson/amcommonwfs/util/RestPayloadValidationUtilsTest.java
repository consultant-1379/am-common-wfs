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
package com.ericsson.amcommonwfs.util;

import static com.ericsson.amcommonwfs.util.RestPayloadValidationUtils.validateJson;
import static com.ericsson.amcommonwfs.util.UnitTestUtils.readDataFromFile;
import static com.toomuchcoding.jsonassert.JsonAssertion.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.ericsson.workflow.orchestration.mgmt.model.v3.InstantiateInfo;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolationException;

public class RestPayloadValidationUtilsTest {

    @Test
    public void testValidateJsonWithoutReleaseName() {
        String instRequest = readDataFromFile(RestPayloadValidationUtilsTest.class, "wfsInstantiateInstance.json");
        assertThatNoException().isThrownBy(() -> validateJson(instRequest, InstantiateInfo.class));
    }

    @Test
    public void testValidateJsonWithoutLifecycleOperationId() {
        String instRequest = readDataFromFile(RestPayloadValidationUtilsTest.class, "wfsInstantiateInstance.json");
        String requestWithoutParam = instRequest.replaceAll(".*\"lifecycleOperationId\":.\"[a-z-]*\",\n", "");
        assertThatThrownBy(() -> validateJson(requestWithoutParam, InstantiateInfo.class))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("lifecycleOperationId: LIFECYCLE_OPERATION_ID_NULL");
    }

    @Test
    public void testValidateJsonWithoutState() {
        String instRequest = readDataFromFile(RestPayloadValidationUtilsTest.class, "wfsInstantiateInstance.json");
        String requestWithoutParam = instRequest.replaceAll(".*\"state\":.\"[a-z]*\",\n", "");
        assertThatThrownBy(() -> validateJson(requestWithoutParam, InstantiateInfo.class))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("state: STATE_NULL");
    }

    @Test
    public void testValidateJsonWithNull() {
        String instRequest = "random String";
        assertThatThrownBy(() -> validateJson(instRequest, InstantiateInfo.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unable to parse request json because of");
    }

    @Test
    public void validYaml() throws URISyntaxException, IOException {
        Path valuesFile = UnitTestUtils.getResource("valueFiles/values.yaml");
        JSONObject parsedYaml = RestPayloadValidationUtils.validateYamlCanBeParsed(Files.newInputStream(valuesFile));
        assertThat(parsedYaml.toString()).contains("replicaCount");
    }

    @Test
    public void catchScannerExceptionWhenInvalidYaml() throws URISyntaxException {
        Path valuesFile = UnitTestUtils.getResource("valueFiles/scannerException.yaml");
        assertThatThrownBy(() -> RestPayloadValidationUtils.validateYamlCanBeParsed(Files.newInputStream(valuesFile))).isInstanceOf(IllegalArgumentException.class).hasMessageStartingWith("Unable to parse yaml file due to [while scanning a simple key");
    }

    @Test
    public void catchInvalidKeyExceptionWhenInvalidYaml() throws URISyntaxException {
        Path valuesFile = UnitTestUtils.getResource("valueFiles/invalidKeyException.yaml");
        assertThatThrownBy(() -> RestPayloadValidationUtils.validateYamlCanBeParsed(Files.newInputStream(valuesFile))).isInstanceOf(IllegalArgumentException.class).hasMessageStartingWith("Unable to parse yaml file due to [Invalid key for njvike");
    }

    @Test
    public void catchExceptionWhenEmptyYaml() throws URISyntaxException {
        Path valuesFile = UnitTestUtils.getResource("valueFiles/emptyValues.yaml");
        assertThatThrownBy(() -> RestPayloadValidationUtils.validateYamlCanBeParsed(Files.newInputStream(valuesFile))).isInstanceOf(IllegalArgumentException.class).hasMessageStartingWith("Unable to parse yaml file due to [Empty file");
    }

    @Test
    public void catchExceptionWhenDuplicateKeyYaml() throws URISyntaxException {
        Path valuesFile = UnitTestUtils.getResource("valueFiles/duplicate_keys_values.yaml");
        assertThatThrownBy(() -> RestPayloadValidationUtils.validateYamlCanBeParsed(Files.newInputStream(valuesFile))).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("duplicate key");
    }

    @Test
    public void catchExceptionWhenNullStream(){
        assertThatThrownBy(() -> RestPayloadValidationUtils.validateYamlCanBeParsed(null)).isInstanceOf(IllegalArgumentException.class).hasMessageStartingWith("Unable to parse yaml file due to [java.io.IOException");
    }
}
