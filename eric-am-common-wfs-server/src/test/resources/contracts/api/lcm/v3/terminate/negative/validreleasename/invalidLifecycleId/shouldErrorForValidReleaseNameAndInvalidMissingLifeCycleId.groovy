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
package contracts.api.lcm.v3.terminate.negative.validreleasename.invalidLifecycleId

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents an error scenario of terminating an Application

```
given:
  client requests to terminate a resource
when:
  the release name is valid and the lifecycleOperationId query parameter is missing
then:
  the request is rejected
```

"""
    )
    request {
        method 'POST'
        urlPath($(regex("/api/lcm/v3/resources/[a-z]([-a-z0-9]*[a-z0-9])?/terminate"))) {
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
