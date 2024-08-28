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
package contracts.api.internal.post.positive.scaleDown

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a failed scenario of scaling down an instantiated resource with missing clusterName

```
given:
  client requests to scale a resource down
when:
  an invalid scale down request is submitted with missing clusterName in message Body
then:
  the request is a bad request
```

""")
    request {
        method 'POST'
        url '/api/internal/kubernetes/pods/scale/down'
        headers {
            contentType(applicationJson())
            header("Idempotency-key", $(regex(nonEmpty())))
        }
        body(
                """
            {
                "releaseName": "test-release",
                "namespace": "test-namespace",
                "applicationTimeOut": "300",
                "lifecycleOperationId": "operation-id"
            }
            """
        )
    }
    response {
        status BAD_REQUEST()
    }
    priority 4
}
