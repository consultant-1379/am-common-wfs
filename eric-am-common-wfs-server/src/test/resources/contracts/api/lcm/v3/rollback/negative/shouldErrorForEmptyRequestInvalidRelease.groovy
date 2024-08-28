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
package contracts.api.lcm.v3.rollback.negative

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents an unsuccessful scenario of rollback of an Application

```
given:
  client requests to rollback a resource
when:
  the request is empty
then:
  the request is rejected
```

"""
    )
    request {
        method 'POST'
        url "/api/lcm/v3/resources/${value(consumer(regex(/[a-z0-9]*([-a-z-A-Z-0-9]*[a-zA-Z0-9])?(\.[a-zA-Z0-9]([-a-zA-Z0-9]*[a-zA-Z0-9])?)*[A-Z]+([-a-z-A-Z-0-9]*[a-zA-Z0-9])*/)))}/rollback"
        body(
                """
{

}
"""
        )
        headers {
            contentType(applicationJson())
            header("Idempotency-key", $(regex(nonEmpty())))
        }

    }
    response {
        status BAD_REQUEST()
        body(
                """
                {
                    "errorDetails": [
                          {
                            "parameterName": "revisionNumber",
                            "message": "revisionNumber field cannot be null or blank"
                          },
                           {
                             "parameterName": "lifecycleOperationId",
                             "message": "lifecycleOperationId cannot be null"
                           },
                           {
                             "parameterName": "state",
                             "message": "state cannot be null"
                           }
                    ]
                }
            """

        )
        headers {
            contentType(applicationJson())
        }
    }
}
