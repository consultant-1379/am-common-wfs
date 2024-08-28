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
package contracts.api.internal.post.negative.pvcs

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents an error scenario of deleting pvcs with missing lifecycle id

```
given:
  client requests to  delete pvcs
when:
  the release name is valid and the lifecycleOperationId query parameter is missing
then:
  the request is rejected
```

"""
    )
    request {
        method 'POST'
        urlPath($(regex("/api/internal/kubernetes/pvcs/[a-z]([-a-z0-9]*[a-z0-9])?/delete"))) {
            queryParameters {
                parameter 'state': 'test'
            }
        }
        headers {
            header("Idempotency-key", $(regex(nonEmpty())))
        }
    }
    response {
        status BAD_REQUEST()
        body("""
                {
    "errorDetails": [
        {
            "parameterName": "lifecycleOperationId",
            "message": "Required request parameter 'lifecycleOperationId' for method parameter type String is not present"
        }
    ]
}
"""
        )
    }
    priority(20)
}
