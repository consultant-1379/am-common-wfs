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
package contracts.api.lcm.v3.get.releasehistory.negative.commandTimedOut

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents an unsuccessful scenario of requesting the release history of a resource

```
given:
  client requests the release history of a resource
when:
  request was made with an command timeOut of 1s
then:
  an error message is returned
```

""")
    request {
        method 'GET'
        urlPath("/api/lcm/v3/resources/release-cmdtimeout") {
            queryParameters {
                parameter 'instanceId': 'dummy-id'
            }
        }
    }
    response {
        status OK()
        body (file("commandTimedOut.json"))
        bodyMatchers {
            jsonPath('$.workflowQueries[0].releaseName', byRegex(/[a-z]([-a-z0-9]*[a-z0-9])?/))
            jsonPath('$.workflowQueries[0].message', byCommand("assertThat(parsedJson.read(\"\$.workflowQueries[0].message\", String.class)).isEqualTo(\"{\\\"detail\\\":\\\"Unable to get the result in the time specified\\\",\\\"status\\\":\\\"422\\\"}\")"))
        }
        headers {
            contentType(applicationJson())
        }
    }
    priority 1
}
