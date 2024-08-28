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

package contracts.api.lcm.v3.rollback.negative.invalidstate

import org.springframework.cloud.contract.spec.Contract

Contract.make {
  description("""
Represents an unsuccessful scenario of rollback of an Application

```
given:
  client requests to rollback a resource
when:
  the revision number is invalid with missing state
then:
  the request is rejected
```

""")
  request {
    method 'POST'
    url "/api/lcm/v3/resources/${value(consumer(regex(/[a-z]([-a-z0-9]*[a-z0-9])?/)))}/rollback"
    body(
            "clusterName": "mycluster",
            "lifecycleOperationId": "my-id"
    )
    headers {
      contentType(applicationJson())
      header("Idempotency-key", $(regex(nonEmpty())))
    }
    bodyMatchers {
      jsonPath('$.clusterName', byRegex('[a-zA-Z0-9]+(\\.config)?'))
      jsonPath('$.lifecycleOperationId', byRegex(nonEmpty()).asString())
    }
  }
  response {
    status BAD_REQUEST()

    body("""
              {
    "errorDetails": [
        {
            "parameterName": "state",
            "message": "state cannot be null"
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
  priority 8
}
