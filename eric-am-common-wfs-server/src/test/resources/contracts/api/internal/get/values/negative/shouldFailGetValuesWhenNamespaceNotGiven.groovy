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
package contracts.api.internal.get.values.negative

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a failure scenario of getting values in a release

```
given:
  client requests to  get values in a release
when:
  the release name is valid and the namespace is invalid
then:
  the request is rejected
```

""")
    request {
        method 'GET'
        urlPath($(regex('/api/internal/kubernetes/values/[a-z]([-a-z0-9]*[a-z0-9])?'))) {
            queryParameters {
                parameter 'clusterName': value(consumer(matching("default")), producer("default"))
            }
        }
        headers {
            contentType(applicationJson())
        }
    }
    response {
        status BAD_REQUEST()
        body (file("namespaceNotGiven.json"))
        headers {
            contentType(applicationJson())
        }
    }
    priority 2
}
