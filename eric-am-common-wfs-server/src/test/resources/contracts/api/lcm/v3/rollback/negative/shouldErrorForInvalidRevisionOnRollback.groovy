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
  the revision number is invalid
then:
  the request is rejected
```

""")
    request {
        method 'POST'
        url "/api/lcm/v3/resources/${value(consumer(regex(/[a-z]([-a-z0-9]*[a-z0-9])?/)))}/rollback"
        body(
                "revisionNumber": 'test',
                "lifecycleOperationId": "my-id",
                "state": "starting"
        )
        headers {
            contentType(applicationJson())
            header("Idempotency-key", $(regex(nonEmpty())))
        }
        bodyMatchers {
            jsonPath('$.revisionNumber', byRegex('^[A-Za-z]+$'))
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
