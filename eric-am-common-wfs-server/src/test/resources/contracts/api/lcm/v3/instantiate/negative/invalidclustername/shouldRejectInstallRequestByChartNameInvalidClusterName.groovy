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
package contracts.api.lcm.v3.instantiate.negative.invalidclustername

import org.springframework.cloud.contract.spec.Contract

Contract.make {
  description("""
Represents an unsuccessful scenario of instantiating an Application

```
given:
  client requests to instantiate a resource
when:
  an invalid instantiate request submitted using an invalid cluster name
then:
  the request is rejected
```

""")
  request {
    method 'POST'
    url "/api/lcm/v3/resources/${value(consumer(regex(/[a-z]([-a-z0-9]*[a-z0-9])?/)))}/instantiate"
    body(
            "chartName": "a",
            "clusterName": "MY;cluster",
            "lifecycleOperationId": "my-id",
            "state": "starting"
    )
    headers {
      contentType(applicationJson())
      header("Idempotency-key", $(regex(nonEmpty())))
    }
    bodyMatchers {
      jsonPath('$.chartName', byRegex(nonEmpty()))
      jsonPath('$.clusterName', byRegex('[a-zA-Z0-9;]+(\\.config)?'))
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
            "parameterName": "clusterName",
            "message": "clusterName must consist of alphanumeric characters. It can be given as just the name or ending with .config"
        }
    ]
}
"""
      )
      headers {
        contentType(applicationJson())
      }
    }
  priority 6
  }
