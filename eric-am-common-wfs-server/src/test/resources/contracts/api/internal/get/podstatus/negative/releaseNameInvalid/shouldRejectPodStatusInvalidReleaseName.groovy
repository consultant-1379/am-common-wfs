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
package contracts.api.internal.get.podstatus.negative.releaseNameInvalid

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a unsuccessful scenario of requesting the pod status of a resource

```
given:
  client requests the pod status of a specified resource
when:
  request is made with a release name that does not exist and a valid cluster name
then:
  an error message is returned
```

""")
    request {
        method GET()
        urlPath($(regex("/api/internal/[a-zA-Z][A-Z]+([-a-zA-Z0-9]*[a-zA-Z0-9])?/pods"))) {
            queryParameters {
                parameter 'clusterName': value(consumer(matching("[a-zA-Z0-9][-a-zA-Z0-9]+")), producer("default"))
            }
        }
    }
    response {
        status BAD_REQUEST()
        body (file("invalidReleaseNameResponse.json"))
        headers {
            contentType(applicationJson())
        }
    }
    priority(2)
}
