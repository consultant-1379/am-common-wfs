/*******************************************************************************
 * COPYRIGHT Ericsson 2021
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
 ******************************************************************************/
package contracts.api.lcm.v3.get.releasehistory.positivereleasename

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario of requesting the release history of a resource

```
given:
  client requests the release history of a resource
when:
  valid request is made with valid release name
then:
  the release History of the specified resource is returned
```

""")
    request {
        method 'GET'
        url "/api/lcm/v3/resources/${value(consumer(regex(/[a-z]([-a-z0-9]*[a-z0-9])?/)), producer("my-release"))}"
    }
    response {
        status OK()
        body (
                """
            {
                "workflowQueries": [
                    {
                        "instanceId": "04a3761b-36a5-11e9-a5ac-96b34b4a5326-dummyId",
                        "definitionKey": "InstantiateApplication__top",
                        "chartName": "adp-am/my-release",
                        "chartUrl": null,
                        "chartVersion": "my-release-0.0.1-223",
                        "releaseName": "${fromRequest().path(4)}",
                        "namespace": "default",
                        "userId": "UNKNOWN",
                        "workflowState": "COMPLETED",
                        "message": "Application deployed with name my-release",
                        "startTime": "1551880124728",
                        "additionalParams": {
                        "es.timeout": "66s"
                        },
                        "revision": "1",
                        "revisionDescription": "Install complete"
                    },
                    {
                        "instanceId": "32cd2585-44b7-11e9-806b-645d86898946-dummyId",
                        "definitionKey": "UpgradeApplication__top",
                        "chartName": "adp-am/my-release",
                        "chartUrl": null,
                        "chartVersion": "my-release-0.0.1-224",
                        "releaseName": "${fromRequest().path(4)}",
                        "namespace": "default",
                        "userId": "UNKNOWN",
                        "workflowState": "COMPLETED",
                        "message": "Application upgraded with name my-release",
                        "startTime": "1551890124328",
                        "additionalParams": {
                        "es.timeout": "56s"
                        },
                        "revision": "2",
                        "revisionDescription": "Upgrade complete"
                    },
                    {
                        "instanceId": "04a3761b-36a5-11e9-a5ac-96b34b4a5326-dummyId",
                        "definitionKey": "RollbackApplication__top",
                        "chartName": "adp-am/my-release",
                        "chartUrl": null,
                        "chartVersion": "my-release-0.0.1-223",
                        "releaseName": "${fromRequest().path(4)}",
                        "namespace": "default",
                        "userId": "UNKNOWN",
                        "workflowState": "COMPLETED",
                        "message": "Application rolled back with name my-release",
                        "startTime": "1551880124829",
                        "additionalParams": {
                        "es.timeout": "76s"
                        },
                        "revision": "3",
                        "revisionDescription": "Rollback complete"
                    }
                ],
                "metadata": {
                "count": 3
                }
            }
                         """
        )
        bodyMatchers {
            jsonPath('$.workflowQueries[0].releaseName', byRegex(/[a-z]([-a-z0-9]*[a-z0-9])?/))
        }
        headers {
            contentType(applicationJson())
        }
    }
}
