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

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CamundaEngineHistoryTtlControl {

    private final int historyTimeToLive;
    private final ProcessEngine processEngine;

    public CamundaEngineHistoryTtlControl(@Value("${camunda.historyTimeToLive}") int historyTimeToLive,
                                          ProcessEngine processEngine) {
        this.historyTimeToLive = historyTimeToLive;
        this.processEngine = processEngine;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void configureDefaultHistoryTimeToLive() {
        RepositoryService repositoryService = processEngine.getRepositoryService();

        List<ProcessDefinition> processDefinitions = processEngine.getRepositoryService()
                .createProcessDefinitionQuery()
                .list();

        processDefinitions.forEach(it -> repositoryService.updateProcessDefinitionHistoryTimeToLive(it.getId(), historyTimeToLive));
    }
}
