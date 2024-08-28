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
package contracts.api.internal.get.podstatus.negative.clusterNameNotGiven

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a unsuccessful scenario of requesting the pod status of a resource

```
given:
  client requests the pod status of a specified resource
when:
  request is made with valid release name and invalid cluster name
then:
  an error message is returned
```

""")
    request {
        method GET()
        url($(regex("/api/internal/[a-z]([-a-z0-9]*[a-z0-9])?/pods")))
    }
    response {
        status BAD_REQUEST()
        body (file("clusterNameNotGivenResponse.json"))
        headers {
            contentType(applicationJson())
        }
    }
    priority(3)
}
