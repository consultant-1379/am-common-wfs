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
Represents an error scenario of upgrading an Application

```
given:
  client requests to upgrade a resource
when:
  the release name is invalid, release name invalid
then:
  the request is rejected
```

"""
    )
    request {
        method 'POST'
        url "/api/lcm/v3/resources/${value(consumer(regex(/[a-z0-9]*([-a-z-A-Z-0-9]*[a-zA-Z0-9])?(\.[a-zA-Z0-9]([-a-zA-Z0-9]*[a-zA-Z0-9])?)*[A-Z]+([-a-z-A-Z-0-9]*[a-zA-Z0-9])*/)))}/upgrade"
        body(
                "chartName": "adp-am/my-chart",
                "chartVersion": "1.2.3",
                "lifecycleOperationId": "my-id",
                "state": "starting",
                "namespace" : "default"
        )
        headers {
            contentType(applicationJson())
            header("Idempotency-key", $(regex(nonEmpty())))
        }
        bodyMatchers {
            jsonPath('$.chartName', byRegex(nonEmpty()))
            jsonPath('$.chartVersion', byRegex(nonEmpty()))
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
    priority 20
}
