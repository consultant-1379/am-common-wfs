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
package contracts.api.internal.post.negative.pvcs

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a failed scenario of deleting pvcs with missing releaseName

```
given:
  client requests to delete pvcs
when:
  the release name is invalid
then:
  the request is rejected
```

"""
    )
    request {
        method 'POST'
        urlPath($(regex("/api/internal/kubernetes/pvcs/[a-z0-9]*([-a-z-A-Z-0-9]*[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([-a-zA-Z0-9]*[a-zA-Z0-9])?)*[A-Z]+([-a-z-A-Z-0-9]*[a-zA-Z0-9])*/delete"))) {
            queryParameters {
                parameter 'lifecycleOperationId': value(consumer(regex(nonEmpty())), producer("my-id"))
                parameter 'state': value(consumer(regex(nonEmpty())), producer("starting"))
                parameter 'namespace': value(consumer(regex(nonEmpty())), producer("default"))
                parameter 'clusterName': value(consumer(regex(nonEmpty())) ,producer("my-cluster"))
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
                      }
                    ]
                }
            """

        )
        headers {
            contentType(applicationJson())
        }
    }
    priority(1)
}
