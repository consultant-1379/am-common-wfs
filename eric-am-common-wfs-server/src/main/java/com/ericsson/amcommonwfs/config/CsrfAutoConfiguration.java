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

import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CsrfAutoConfiguration {
    private static final String CSRF_PREVENTION_FILTER = "CsrfPreventionFilter";

    /**
     * Overwrite csrf filter from Camunda configured here
     * org.camunda.bpm.spring.boot.starter.webapp.CamundaBpmWebappInitializer
     * org.camunda.bpm.spring.boot.starter.webapp.filter.SpringBootCsrfPreventionFilter
     * Is configured with basically a 'no-op' filter
     */
    @Bean
    public ServletContextInitializer csrfOverwrite() {
        return servletContext -> servletContext
                .addFilter(CSRF_PREVENTION_FILTER, (request, response, chain) -> chain.doFilter(request, response));
    }
}
