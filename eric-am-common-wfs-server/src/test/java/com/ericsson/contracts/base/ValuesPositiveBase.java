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
package com.ericsson.contracts.base;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import java.util.HashMap;
import java.util.Map;
import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.amcommonwfs.ApplicationServer;
import com.ericsson.amcommonwfs.presentation.services.HelmService;
import com.ericsson.amcommonwfs.utility.DataParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@ActiveProfiles("dev")
@SpringBootTest(classes = ApplicationServer.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = {"spring.flyway.enabled=false"})
@AutoConfigureObservability
public class ValuesPositiveBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValuesPositiveBase.class);

    @MockBean
    private HelmService helmService;

    @Inject
    private WebApplicationContext context;

    @BeforeEach
    public void setup() throws JsonProcessingException {
        given(helmService.getValues(anyString(), anyString(), anyString(), anyString()))
                .willReturn(parseResponseFile("allValuesInRelease"));
        given(helmService.getValues(eq("end-to-end-sync-success-1"), anyString(), anyString(), any()))
                .willReturn(parseResponseFile("allValuesInSyncSuccessReleaseChart1"));
        given(helmService.getValues(eq("end-to-end-sync-success-2"), anyString(), anyString(), any()))
                .willReturn(parseResponseFile("allValuesInSyncSuccessReleaseChart2"));
        given(helmService.getValues(eq("end-to-end-sync-failure-1"), anyString(), anyString(), any()))
                .willReturn(parseResponseFile("allValuesInSyncFailureReleaseChart1"));
        given(helmService.getValues(eq("end-to-end-sync-failure-2"), anyString(), anyString(), any()))
                .willReturn(parseResponseFile("allValuesInSyncFailureReleaseChart2"));
        RestAssuredMockMvc.webAppContextSetup(context);
    }

    private Map<String, Object> parseResponseFile(final String fileName) throws JsonProcessingException {
        LOGGER.info("in mock");
        String responseTemplate = DataParser.readFile(String.format("contracts/api/internal/get/values/positive/%s.json", fileName));
        LOGGER.info("JSON IS {}", responseTemplate);
        ObjectMapper mapper = new ObjectMapper();
        TypeReference<HashMap<String, Object>> MAP_TYPE = new TypeReference<>() {
        };
        return mapper.readValue(responseTemplate, MAP_TYPE);
    }
}
