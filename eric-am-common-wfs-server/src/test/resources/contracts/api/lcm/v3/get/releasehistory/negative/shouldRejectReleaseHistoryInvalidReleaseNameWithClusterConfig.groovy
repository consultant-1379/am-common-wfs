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
package contracts.api.lcm.v3.get.releasehistory.negative

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents an unsuccessful scenario of requesting the release history of a resource

```
given:
  client requests the release history of a resource
when:
  request is made with an invalid release name with a cluster config
then:
  an error message is returned
```

""")
    request {
        method 'GET'
        urlPath($(regex("/api/lcm/v3/resources/[a-z0-9]*([-a-zA-Z0-9]*)[a-z0-9]*([-a-zA-Z0-9]*[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([-a-zA-Z0-9]*[a-zA-Z0-9])?)*[A-Z]+([-a-zA-Z0-9]*[a-zA-Z0-9])*"))) {
            queryParameters {
                parameter 'clusterName': value(consumer(matching("[a-zA-Z0-9][-a-zA-Z0-9]+")), producer("default"))
            }
        }
    }
    response {
        status BAD_REQUEST()
        body(file("invalidReleaseNameResponse.json"))
        headers {
            contentType(applicationJson())
        }
    }
    priority 1
}
