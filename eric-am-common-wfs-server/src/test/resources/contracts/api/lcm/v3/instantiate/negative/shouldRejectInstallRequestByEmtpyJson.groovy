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
package contracts.api.lcm.v3.instantiate.negative

import org.springframework.cloud.contract.spec.Contract

Contract.make {
  description("""
Represents an unsuccessful scenario of instantiating an Application

```
given:
  client requests to instantiate a resource
when:
  an invalid instantiate request submitted
then:
  the request is rejected
```

""")
  request {
    method 'POST'
    url "/api/lcm/v3/resources/${value(consumer(regex(/[a-z]([-a-z0-9]*[a-z0-9])?/)))}/instantiate"
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

      body("""
{
    "errorDetails": [
        {
            "parameterName": "state",
            "message": "state cannot be null"
        },
        {
            "parameterName": "chartName, chartUrl",
            "message": "Either chartUrl or chartName is required"
        },
        {
            "parameterName": "lifecycleOperationId",
            "message": "lifecycleOperationId cannot be null"
        }
    ]
}
"""
      )
      headers {
        contentType(applicationJson())
      }
    }
  priority 20
  }
