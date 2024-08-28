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
Represents a successful scenario of scaling down an instantiated resource

```
given:
  client requests to scale a resource down
when:
  valid scale down request is submitted
then:
  the request is accepted
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
                "releaseName": "${value(consumer(regex("[a-z]([-a-z0-9]*[a-z0-9])?")), producer("my-release"))}",
                "clusterName": "${value(consumer(regex("[a-zA-Z0-9][-a-zA-Z0-9]+")), producer("default"))}",
                "namespace": "${value(consumer(regex("[a-z0-9]([-a-z0-9]*[a-z0-9])?")), producer("test-namespace"))}",
                "lifecycleOperationId": "${value(consumer(regex("[a-z0-9][-a-z0-9]+[a-z0-9]")), producer("lifecycle-operation-id"))}",
                "applicationTimeOut": "${value(consumer(regex("[0-9]+")), producer("50"))}"
            }
            """
        )
    }
    response {
        status ACCEPTED()
    }
    priority 3
}
