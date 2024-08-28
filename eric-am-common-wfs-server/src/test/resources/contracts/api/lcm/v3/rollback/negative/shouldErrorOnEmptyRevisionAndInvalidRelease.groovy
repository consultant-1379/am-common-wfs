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
    description(
            """
Represents an unsuccessful scenario of rollback of an Application

```
given:
  client requests to rollback a resource
when:
  the release name and the revision are invalid
then:
  the request is rejected
```

"""
    )
    request {
        method 'POST'
        url "/api/lcm/v3/resources/${value(consumer(regex(/[a-z0-9]*([-a-z-A-Z-0-9]*[a-zA-Z0-9])?(\.[a-zA-Z0-9]([-a-zA-Z0-9]*[a-zA-Z0-9])?)*[A-Z]+([-a-z-A-Z-0-9]*[a-zA-Z0-9])*/)))}/rollback"
        body(
            "revisionNumber": "",
                "lifecycleOperationId": "my-id",
                "state": "starting"
        )
        headers {
            contentType(applicationJson())
            header("Idempotency-key", $(regex(nonEmpty())))
        }
        bodyMatchers {
            jsonPath('$.lifecycleOperationId', byRegex(nonEmpty()).asString())
            jsonPath('$.state', byRegex(nonEmpty()).asString())
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
                            "message": "revisionNumber field must be numeric"
                        },
                        {
                            "parameterName": "revisionNumber",
                            "message": "revisionNumber field cannot be null or blank"
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
