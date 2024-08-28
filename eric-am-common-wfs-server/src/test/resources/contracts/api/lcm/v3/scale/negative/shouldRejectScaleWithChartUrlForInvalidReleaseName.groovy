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

package contracts.api.lcm.v3.scale.negative

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents an unsuccessful scenario of scaling an Application

```
given:
  client requests to scale a resource
when:
  the release name is invalid, release name invalid
then:
  the request is rejected
```

"""
    )
    request {
        method 'POST'
        url "/api/lcm/v3/resources/${value(consumer(regex(/[a-z0-9]*([-a-z-A-Z-0-9]*[a-zA-Z0-9])?(\.[a-zA-Z0-9]([-a-zA-Z0-9]*[a-zA-Z0-9])?)*[A-Z]+([-a-z-A-Z-0-9]*[a-zA-Z0-9])*/)))}/scale"
        body(
                """
                {
                  "chartUrl": "https://arm.rnd.ki.sw.ericsson.se/artifactory/proj-adp-spider-team-helm/spider-app/spider-app-2.74.8.tgz",
                  "lifecycleOperationId": "my-id",
                  "state": "starting",
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
            jsonPath('$.chartUrl', byRegex(nonEmpty()))
            jsonPath('$.lifecycleOperationId', byRegex(nonEmpty()).asString())
            jsonPath('$.state', byRegex(nonEmpty()).asString())
            jsonPath('$.namespace', byRegex(nonEmpty()).asString())
        }
    }
    response {
        status BAD_REQUEST()
        body(
                """
                {
                "errorDetails": [
                      {
                        "parameterName": "releaseName",
                        "message": "releaseName must consist of lower case alphanumeric characters or -. It must start with an alphabetic character, and end with an alphanumeric character"
                      }
                    ]
                }
            """

        )
        headers {
            contentType(applicationJson())
        }
    }
    priority 11

}
