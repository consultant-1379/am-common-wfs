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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;


@ExtendWith(SpringExtension.class)
public class JWTDecoderTest {

    ObjectMapper objectMapper;

    @Test
    public void testDecodeToMapWithInvalidToken(){
        String dummyToken = "asdasww";
        assertThat(JWTDecoder.decodeToMap(dummyToken, objectMapper)).isEqualTo(Optional.empty());
    }

    @Test
    public void testDecodeToMapWithNullToken(){
        String dummyToken = null;
        assertThat(JWTDecoder.decodeToMap(dummyToken, objectMapper)).isEqualTo(Optional.empty());
    }

    @Test
    public void testDecodeToMapWithEmptyObjectMapper() {
        String dummyToken = "fge3235.randomToken.234cdssdg";
        objectMapper = new ObjectMapper();
        Map<String, Object> emptyMap = new HashMap<>();
        assertThat(JWTDecoder.decodeToMap(dummyToken, objectMapper)).isEqualTo(Optional.of(emptyMap));
    }

    @Test
    public void testDecodeToMapWithRealToken() {
        String realToken = "Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJIQjFscG9jMGJCWG9NdHF2ZGZzc3ZMSzFiRFQ0MF9tNXpPa0g2d202Ym9vIn0." +
                "eyJleHAiOjE1OTEzNjkyNjcsImlhdCI6MTU5MTM2OTIwNywianRpIjoiMGNmMTVjZWEtZmVlNi00MTk1LWE5OWYtNWVlYThjYTljYjViIiwiaXNzIjoiaHR0cHM6Ly9pYW0tY29" +
                "kZXBsb3kub3JjaC1ydi1jN2EyLmF0aHRlbS5lZWkuZXJpY3Nzb24uc2UvYXV0aC9yZWFsbXMvbWFzdGVyIiwiYXVkIjpbIm1hc3Rlci1yZWFsbSIsImFjY291bnQiXSwic3ViIjoiZ" +
                "DI0OWNhNDgtNjExNy00ZTVlLWIyMGMtN2MwNGNkOGNjN2YwIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoiZW8iLCJzZXNzaW9uX3N0YXRlIjoiOWM2NDI0YWYtMTY0My00NzA5LWIwZmEtOGY" +
                "wNzFiNjBkMDNhIiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6WyJodHRwczovL2VvLWV2bmZtLm9yY2gtcnYtYzdhMi5hdGh0ZW0uZWVpLmVyaWNzc29uLnNlIiwiaHR0cHM6Ly9pYW0t" +
                "Y29kZXBsb3kub3JjaC1ydi1jN2EyLmF0aHRlbS5lZWkuZXJpY3Nzb24uc2UiLCJodHRwczovL2VvLXNvLm9yY2gtcnYtYzdhMi5hdGh0ZW0uZWVpLmVyaWNzc29uLnNlIl0sInJlYWxtX2FjY2Vz" +
                "cyI6eyJyb2xlcyI6WyJCYWNrdXAgYW5kIFJlc3RvcmUiLCJVc2VyQWRtaW4iLCJvZmZsaW5lX2FjY2VzcyIsIlZNIFZORk0gV0ZTIiwiRS1WTkZNIFN1cGVyIFVzZXIgUm9sZSIsIk1ldHJpY3NWaWV" +
                "3ZXIiLCJMb2dWaWV3ZXIiLCJ1bWFfYXV0aG9yaXphdGlvbiIsIlZNIFZORk0gVklFVyBXRlMiXX0sInNjb3BlIjoiZW1haWwgcHJvZmlsZSIsInJlc291cmNlX2FjY2VzcyI6eyJtYXN0ZXItcmVhbG" +
                "0iOnsicm9sZXMiOlsibWFuYWdlLXJlYWxtIiwibWFuYWdlLXVzZXJzIl19LCJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2Z" +
                "pbGUiXX19LCJ0ZW5hbnRfbmFtZSI6Im1hc3RlciIsImVtYWlsX3ZlcmlmaWVkIjpmYWxzZSwicHJlZmVycmVkX3VzZXJuYW1lIjoidm5mbS11c2VyIn0.Xa8f0Cx-37SRwnqOOpPm4yfNa5PagW4QgfjT1" +
                "Garh-xOeGNZIobGeeMIEbhAvoGredPQTW2G_MiL0OgiF4ikyomxOhIBHb3m2eXHAAx17CMsifGgJjeGchJHovfLTuIamlYWq84mnB3s-OvBXf3zFbFhlpNMz508Q1D19ZXSFhvKRYGBiLg9cx2_nb67HgFX0" +
                "MWtt9Pal6WFug5KnjrTYCIgYMnjLZPlCqHAf9uasLqWyTL2y48Xs4eOsHwGyOrFmMfI2ywECfuMQ_swbD6dE0QjJpkobZ6lVsnZvDM9x6TYaoptCTy1WckSJPEw82XABzERycQJObWxDiVNV_aPuw";
        objectMapper = new ObjectMapper();
        assertThat(JWTDecoder.decodeToMap(realToken, objectMapper).get()).containsKey("exp");
    }
}
