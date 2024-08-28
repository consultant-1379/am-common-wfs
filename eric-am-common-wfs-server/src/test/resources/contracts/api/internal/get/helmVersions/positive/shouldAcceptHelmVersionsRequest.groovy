/*******************************************************************************
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
 ******************************************************************************/
package contracts.api.internal.get.helmVersions.positive

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario of requesting the available helm versions

```
given:
  client requests the available helm versions
when:
  valid request is made
then:
  the available helm versions is returned
```

""")
    request {
        method GET()
        urlPath("/api/internal/helm/versions"){

        }
    }
    response {
        status OK()
        body ((file("validHelmVersionsResponse.json")))
        headers {
            contentType(applicationJson())
        }
    }
    priority(2)
}