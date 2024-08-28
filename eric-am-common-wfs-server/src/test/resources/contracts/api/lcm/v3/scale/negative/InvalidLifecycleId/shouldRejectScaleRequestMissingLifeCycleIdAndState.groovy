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

package contracts.api.lcm.v3.scale.negative.InvalidLifecycleId

import org.springframework.cloud.contract.spec.Contract

Contract.make {
  description("""
Represents an unsuccessful scenario of scale of an Application

```
given:
  client requests to scale a resource
when:
  Request with missing lifecycleOperationId and state
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
      jsonPath('$.chartUrl', byRegex(nonEmpty()).asString())
      jsonPath('$.namespace', byRegex(nonEmpty()).asString())
    }
  }
  response {
    status BAD_REQUEST()

    body("""
              {
    "errorDetails": [
        {
            "parameterName": "lifecycleOperationId",
            "message": "lifecycleOperationId cannot be null"
        },
          {
            "parameterName": "state",
            "message": "state cannot be null"
        }
    ]
}
"""
    )
    headers {
      contentType(applicationJson())
    }
  }
  priority 9
}
