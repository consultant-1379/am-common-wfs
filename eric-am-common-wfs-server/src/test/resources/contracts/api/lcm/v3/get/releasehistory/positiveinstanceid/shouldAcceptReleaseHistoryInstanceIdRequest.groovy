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
package contracts.api.lcm.v3.get.releasehistory.positiveinstanceid

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario of requesting the release history of a resource

```
given:
  client requests the release history of a resource
when:
  valid request is made with valid release name and instanceId
then:
  the release History of the specified resource is returned
```

""")
    request {
        method 'GET'
        urlPath($(regex("/api/lcm/v3/resources/[a-z]([-a-z0-9]*[a-z0-9])?"))) {
            queryParameters {
                parameter 'instanceId': value(consumer(regex(/[a-z0-9]{8}(-[a-z0-9]{4}){3}-[a-z0-9]{12}-([a-zA-Z]*)*/)),
producer("04a3761b-36a5-11e9-a5ac-96b34b4a5326-dummyId"))
            }
        }
    }
    response {
        status OK()
        body (
                """
            {
                "workflowQueries": [
                    {
                        "instanceId": "${fromRequest().path(5)}",
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
                    }
                ],
                "metadata": {
                "count": 1
                }
            }
                         """
        )
        bodyMatchers {
            jsonPath('$.workflowQueries[0].releaseName', byRegex(/[a-z]([-a-z0-9]*[a-z0-9])?/))
            jsonPath('$.workflowQueries[0].instanceId', byRegex(/[a-z0-9]{8}(-[a-z0-9]{4}){3}-[a-z0-9]{12}(-[a-zA-Z]*)*/))
        }
        headers {
            contentType(applicationJson())
        }
    }
}
