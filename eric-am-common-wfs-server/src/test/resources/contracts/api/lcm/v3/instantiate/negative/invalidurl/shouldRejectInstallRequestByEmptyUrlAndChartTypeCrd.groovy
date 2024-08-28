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
package contracts.api.lcm.v3.instantiate.negative.invalidurl

import org.springframework.cloud.contract.spec.Contract

Contract.make {
  description("""
Represents an unsuccessful scenario of instantiating an Application with a CRD resource

```
given:
  client requests to instantiate a CRD resource
when:
  an invalid instantiate request submitted with a chartType = CRD
then:
  the request is rejected
```

""")
  request {
    method 'POST'
    url "/api/lcm/v3/resources/${value(consumer(regex(/[a-z]([-a-z0-9]*[a-z0-9])?/)))}/instantiate"
    body(
            "chartUrl": "",
            "lifecycleOperationId": "my-id",
            "state": "starting",
            "chartType": "CRD"
    )
    headers {
      contentType(applicationJson())
      header("Idempotency-key", $(regex(nonEmpty())))
    }
    bodyMatchers {
      jsonPath('$.lifecycleOperationId', byRegex(nonEmpty()).asString())
      jsonPath('$.state', byRegex(nonEmpty()).asString())
      jsonPath('$.chartType', byRegex("CRD|CNF"))
    }
  }
    response {
      status BAD_REQUEST()

      body("""
              {
    "errorDetails": [
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
  priority 8
  }
