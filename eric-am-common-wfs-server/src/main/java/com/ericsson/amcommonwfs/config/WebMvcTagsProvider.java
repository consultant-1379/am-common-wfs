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
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.server.observation.DefaultServerRequestObservationConvention;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Component
@Slf4j
public class WebMvcTagsProvider extends DefaultServerRequestObservationConvention {

    private static final String URI_TAG_KEY = "uri";
    private static final String UNKNOWN_TAG_VALUE = "UNKNOWN";

    @Override
    public @NotNull KeyValues getLowCardinalityKeyValues(ServerRequestObservationContext context) {
        Iterator<KeyValue> iterator = context.getLowCardinalityKeyValues().iterator();
        return removeUnknownUriTag(iterator);
    }

    @Override
    public @NotNull KeyValues getHighCardinalityKeyValues(ServerRequestObservationContext context) {
        Iterator<KeyValue> iterator = context.getHighCardinalityKeyValues().iterator();
        return removeUnknownUriTag(iterator);
    }

    private static KeyValues removeUnknownUriTag(Iterator<KeyValue> sourceTags) {
        List<KeyValue> resultTags = new ArrayList<>();

        sourceTags.forEachRemaining(keyValue -> {
            if (URI_TAG_KEY.equals(keyValue.getKey()) && UNKNOWN_TAG_VALUE.equals(keyValue.getValue())) {
                LOGGER.info("Removing UNKNOWN uri metric tag");
            } else {
                resultTags.add(keyValue);
            }
        });
        return KeyValues.of(resultTags);
    }
}
