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

package contracts.api.lcm.v3.scale.negative.invalidstate

import org.springframework.cloud.contract.spec.Contract

Contract.make {
  description("""
Represents an unsuccessful scenario of scale of an Application

```
given:
  client requests to scale a resource
when:
  Request with empty state
then:
  the request is rejected
```

""")
  request {
    method 'POST'
    url "/api/lcm/v3/resources/${value(consumer(regex(/[a-z]([-a-z0-9]*[a-z0-9])?/)))}/scale"
    body(
            """
                {
                  "chartUrl": "https://arm.rnd.ki.sw.ericsson.se/artifactory/proj-adp-spider-team-helm/spider-app/spider-app-2.74.8.tgz",
                  "lifecycleOperationId": "my-id",
                  "state": "",
                  "namespace": "default",
                  "scaleResources": {
                      "test-deployment": {
                        "someParameter.replica": 3,
                        "someParameter.minReplica": 3,
                        "someParameter.maxReplica": 3
                      }
                  }
                }
                """
    )
    headers {
      contentType(applicationJson())
      header("Idempotency-key", $(regex(nonEmpty())))
    }
    bodyMatchers {
      
      jsonPath('$.chartUrl', byRegex(url()).asString())
      jsonPath('$.lifecycleOperationId', byRegex(nonEmpty()).asString())
      jsonPath('$.namespace', byRegex(nonEmpty()).asString())
    }
  }
  response {
    status BAD_REQUEST()

    body("""
              {
    "errorDetails": [
        {
            "parameterName": "state",
            "message": "state cannot be empty"
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
