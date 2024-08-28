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
Represents a failed scenario of scaling down an instantiated resource with not found error

```
given:
  client requests to scale a resource down
when:
  valid scale down request is submitted but clusterName is not found
then:
  the request will return as NOT_FOUND
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
                "clusterName": "cluster-not-found",
                "namespace": "test-namespace",
                "applicationTimeOut": "300",
                "lifecycleOperationId": "operation-id"
            }
            """
        )
    }
    response {
        status NOT_FOUND()
    }
    priority 2
}
