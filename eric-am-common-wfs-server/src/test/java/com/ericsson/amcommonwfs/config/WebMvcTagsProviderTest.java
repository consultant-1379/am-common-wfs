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
package com.ericsson.amcommonwfs.config;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class WebMvcTagsProviderTest {

    @InjectMocks
    private WebMvcTagsProvider webMvcTagsProvider;

    @Test
    void testGetLowCardinalityKeyValuesWithUnknown() {
        ServerRequestObservationContext context = getServerRequestObservationContext();
        KeyValue keyValue = KeyValue.of("uri", "UNKNOWN");
        KeyValue keyValue2 = KeyValue.of("wfs-service", "test2");
        KeyValue keyValue3 = KeyValue.of("uri", "UNKNOWN");
        context.addLowCardinalityKeyValues(KeyValues.of(Arrays.asList(keyValue, keyValue2, keyValue3)));

        KeyValues keyValues =
            webMvcTagsProvider.getLowCardinalityKeyValues(context);
        KeyValue actualKeyValue = keyValues.stream()
            .findFirst()
            .orElseThrow(() -> new RuntimeException("KeyValues is empty"));

        assertEquals(1, keyValues.stream().count());
        assertEquals("wfs-service", actualKeyValue.getKey());
        assertEquals("test2", actualKeyValue.getValue());
    }

    @Test
    void testGetLowCardinalityKeyValues() {
        ServerRequestObservationContext context = getServerRequestObservationContext();
        KeyValue keyValue = KeyValue.of("wfs-service", "test2");
        context.addLowCardinalityKeyValue(keyValue);

        KeyValues keyValues =
            webMvcTagsProvider.getLowCardinalityKeyValues(context);
        KeyValue actualKeyValue = keyValues.stream()
            .findFirst()
            .orElseThrow(() -> new RuntimeException("KeyValues is empty"));

        assertEquals(1, keyValues.stream().count());
        assertEquals("wfs-service", actualKeyValue.getKey());
        assertEquals("test2", actualKeyValue.getValue());
    }

    @Test
    void testGetLowCardinalityKeyValuesEmpty() {
        ServerRequestObservationContext context = getServerRequestObservationContext();

        KeyValues keyValues = webMvcTagsProvider.getLowCardinalityKeyValues(context);

        assertEquals(0, keyValues.stream().count());
    }


    @Test
    void testGetHighCardinalityKeyValuesWithUnknown() {
        ServerRequestObservationContext context = getServerRequestObservationContext();
        KeyValue keyValue = KeyValue.of("uri", "UNKNOWN");
        KeyValue keyValue2 = KeyValue.of("wfs-service", "test2");
        KeyValue keyValue3 = KeyValue.of("uri", "UNKNOWN");
        context.addHighCardinalityKeyValues(KeyValues.of(Arrays.asList(keyValue, keyValue2, keyValue3)));

        KeyValues keyValues =
            webMvcTagsProvider.getHighCardinalityKeyValues(context);
        KeyValue actualKeyValue = keyValues.stream()
            .findFirst()
            .orElseThrow(() -> new RuntimeException("KeyValues is empty"));

        assertEquals(1, keyValues.stream().count());
        assertEquals("wfs-service", actualKeyValue.getKey());
        assertEquals("test2", actualKeyValue.getValue());
    }

    @Test
    void testGetHighCardinalityKeyValues() {
        ServerRequestObservationContext context = getServerRequestObservationContext();
        KeyValue keyValue = KeyValue.of("wfs-service", "test2");
        context.addHighCardinalityKeyValue(keyValue);

        KeyValues keyValues =
            webMvcTagsProvider.getHighCardinalityKeyValues(context);
        KeyValue actualKeyValue = keyValues.stream()
            .findFirst()
            .orElseThrow(() -> new RuntimeException("KeyValues is empty"));

        assertEquals(1, keyValues.stream().count());
        assertEquals("wfs-service", actualKeyValue.getKey());
        assertEquals("test2", actualKeyValue.getValue());
    }

    @Test
    void testGetHighCardinalityKeyValuesEmpty() {
        ServerRequestObservationContext context = getServerRequestObservationContext();

        KeyValues keyValues = webMvcTagsProvider.getHighCardinalityKeyValues(context);

        assertEquals(0, keyValues.stream().count());
    }

    private ServerRequestObservationContext getServerRequestObservationContext() {
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.setRequestURI("/mock");

        HttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();
        mockHttpServletResponse.setStatus(200);

        return new ServerRequestObservationContext(mockHttpServletRequest, mockHttpServletResponse);
    }
}
