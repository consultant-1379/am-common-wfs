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
package contracts.api.lcm.v3.upgrade.negative

import org.springframework.cloud.contract.spec.Contract

Contract.make {
  description("""
Represents an unsuccessful scenario of upgrading an Application

```
given:
  client requests to upgrade a resource
when:
  an invalid upgrade request submitted, empty request body
then:
  the request is rejected
```

""")
  request {
    method 'POST'
    url "/api/lcm/v3/resources/${value(consumer(regex(/[a-z]([-a-z0-9]*[a-z0-9])?/)))}/upgrade"
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
            "parameterName": "chartName, chartUrl",
            "message": "Either chartUrl or chartName is required"
        },
         {
            "parameterName": "lifecycleOperationId",
            "message": "lifecycleOperationId cannot be null"
        },
         {
            "parameterName": "state",
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
    priority 18
}
