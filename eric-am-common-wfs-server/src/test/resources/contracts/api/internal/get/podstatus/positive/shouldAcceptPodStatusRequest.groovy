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
package contracts.api.internal.get.podstatus.positive

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario of requesting the pod status of a resource

```
given:
  client requests the pod status of a specified resource
when:
  valid request is made with valid release name
then:
  the status of the pods of the specified resource is returned
```

""")
    request {
        method GET()
        urlPath($(regex("/api/internal/[a-z]([-a-z0-9]*[a-z0-9])?/pods"))){
            queryParameters {
                parameter 'clusterName': value(consumer(matching("[a-zA-Z0-9][-a-zA-Z0-9]+")), producer("default"))
            }
        }
    }
    response {
        status OK()
        body (file("validPodStatusResponse.json"))
        headers {
            contentType(applicationJson())
        }
    }
    priority(2)
}
