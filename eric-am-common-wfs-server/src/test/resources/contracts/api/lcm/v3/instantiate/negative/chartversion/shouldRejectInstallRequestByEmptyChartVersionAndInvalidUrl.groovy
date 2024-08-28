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
package contracts.api.lcm.v3.instantiate.negative.chartversion

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
            "chartVersion": "",
            "chartUrl": "Invalid_url",
            "lifecycleOperationId": "my-id",
            "state": "starting"
    )
    headers {
      contentType(applicationJson())
      header("Idempotency-key", $(regex(nonEmpty())))
    }
    bodyMatchers {
      jsonPath('$.chartUrl', byRegex(nonEmpty()).asString())
      jsonPath('$.lifecycleOperationId', byRegex(nonEmpty()).asString())
      jsonPath('$.state', byRegex(nonEmpty()).asString())
    }
  }
    response {
      status BAD_REQUEST()

      body("""
              {
    "errorDetails": [
        {
            "parameterName": "chartName, chartUrl, chartVersion",
            "message": "If chartUrl property has been specified, chartName and chartVersion properties should not be set. Please see API documentation for correct usage."
        },
         {
            "parameterName": "chartUrl",
            "message": "chartUrl is not a valid format"
        }
    ]
}
"""
      )
      headers {
        contentType(applicationJson())
      }
    }
  priority 3
  }
