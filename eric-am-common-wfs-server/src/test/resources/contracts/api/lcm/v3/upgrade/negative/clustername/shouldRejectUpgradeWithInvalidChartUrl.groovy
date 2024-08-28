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
package contracts.api.lcm.v3.upgrade.negative.clustername

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents an error scenario of upgrading an Application

```
given:
  client requests to upgrade a resource
when:
  the chart url is invalid and the cluster name is valid
then:
  the request is rejected
```

"""
    )
    request {
        method 'POST'
        url "/api/lcm/v3/resources/${value(consumer(regex(/[a-z]([-a-z0-9]*[a-z0-9])?/)))}/upgrade"
        body(
                "chartUrl": "Invalid_url",
                "clusterName": "mycluster",
                "lifecycleOperationId": "my-id",
                "state": "starting"
        )
        headers {
            contentType(applicationJson())
            header("Idempotency-key", $(regex(nonEmpty())))
        }
        bodyMatchers {
            jsonPath('$.chartUrl', byRegex(url()).asString())
            jsonPath('$.clusterName', byRegex('[a-zA-Z0-9]+(\\.config)?'))
            jsonPath('$.lifecycleOperationId', byRegex(nonEmpty()).asString())
            jsonPath('$.state', byRegex(nonEmpty()).asString())
        }
    }
    response {
        status BAD_REQUEST()
        body(
                """
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
    priority 21
}
