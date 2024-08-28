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
package contracts.api.lcm.v3.terminate.negative.invalidreleasename.invalidclustername

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents an error scenario of terminating an Application

```
given:
  client requests to terminate a resource
when:
  the release name is invalid and the cluster name is invalid
then:
  the request is rejected
```

"""
    )
    request {
        method 'POST'
        urlPath($(regex("/api/lcm/v3/resources/[a-z0-9]*([-a-z-A-Z-0-9]*[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([-a-zA-Z0-9]*[a-zA-Z0-9])?)*[A-Z]+([-a-z-A-Z-0-9]*[a-zA-Z0-9])*/terminate"))){
            queryParameters {
                parameter 'clusterName': value(consumer(regex("[a-zA-Z0-9-_]+\\+[a-zA-Z0-9-_]+(\\.config)?+")),producer("my+cluster"))
                parameter 'lifecycleOperationId': value(consumer(regex(nonEmpty())), producer("my-id"))
                parameter 'state': value(consumer(regex(nonEmpty())), producer("starting"))
                parameter 'namespace': value(consumer(regex(nonEmpty())), producer("default"))
            }
        }
        headers {
            header("Idempotency-key", $(regex(nonEmpty())))
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
                      },
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
    priority(4)
}
