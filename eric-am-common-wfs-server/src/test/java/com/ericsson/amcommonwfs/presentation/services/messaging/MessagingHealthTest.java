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
package com.ericsson.amcommonwfs.presentation.services.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.actuate.data.redis.RedisHealthIndicator;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = MessagingHealth.class)
@ActiveProfiles("dev")
@TestPropertySource(properties = {"spring.flyway.enabled=false"})
public class MessagingHealthTest {
    @MockBean
    private RedisHealthIndicator healthIndicator;

    @Autowired
    private MessagingHealth messagingHealth;


    @Test
    public void shouldReturnTrueWhenStatusIsUp() {
        // given
        when(healthIndicator.health()).thenReturn(Health.status(Status.UP).build());

        // when
        final var result = messagingHealth.isUp();

        // then
        assertThat(result).isTrue();
    }

    @Test
    public void shouldReturnTrueWhenStatusIsDown() {
        // given
        when(healthIndicator.health()).thenReturn(Health.status(Status.DOWN).build());

        // when
        final var result = messagingHealth.isUp();

        // then
        assertThat(result).isFalse();
    }
}
