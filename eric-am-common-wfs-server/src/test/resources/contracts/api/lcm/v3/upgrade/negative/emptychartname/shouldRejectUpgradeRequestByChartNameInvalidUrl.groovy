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
package contracts.api.lcm.v3.upgrade.negative.emptychartname

import org.springframework.cloud.contract.spec.Contract

Contract.make {
  description("""
Represents an unsuccessful scenario of upgrading an Application

```
given:
client requests to upgrade a resource

when:
an invalid upgrade request submitted, both chartName and chartUrl are in the request body, chartName is empty, chartUrl has an invalid format

then:
the request is rejected
  ```

""")
  request {
    method 'POST'
    url "/api/lcm/v3/resources/${value(consumer(regex(/[a-z]([-a-z0-9]*[a-z0-9])?/)))}/upgrade"
    body(
            "chartName": "",
            "chartUrl": "Invalid_url",
            "lifecycleOperationId": "my-id",
            "state": "starting"
    )
    headers {
      contentType(applicationJson())
      header("Idempotency-key", $(regex(nonEmpty())))
    }
    bodyMatchers {
      jsonPath('$.chartUrl', byRegex(nonEmpty()))
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
        },
        {
            "parameterName": "chartName",
            "message": "chartName cannot be empty"
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
