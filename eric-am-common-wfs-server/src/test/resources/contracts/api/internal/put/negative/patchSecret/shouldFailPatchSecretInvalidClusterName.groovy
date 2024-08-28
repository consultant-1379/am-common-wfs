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
package contracts.api.internal.put.negative.patchSecret

import org.springframework.cloud.contract.spec.Contract

Contract.make {
  description("""
Represents a failed scenario of patching a secret with an invalid cluster name in message body

```
given:
  client requests to patch a secret
when:
  invalid patch secret request is submitted with an invalid cluster name in message body
then:
  the request is a bad request
```

""")
    request {
        method 'PUT'
        url "/api/internal/kubernetes/secrets/${value(consumer(regex(/[a-z]([-a-z0-9]*[a-z0-9])?/)))}"
        headers {
            contentType(applicationJson())
        }
        body(
                "namespace": "test-patch-secret",
                "clusterName": "invalid-cluster-name*?",
                "key": "secret-key",
                "value": "test value for the secret key"
        )
        bodyMatchers {
            jsonPath('$.namespace', byRegex(/[a-z0-9]([-a-z0-9]*[a-z0-9])?/))
            jsonPath('$.clusterName', byRegex(/[a-zA-Z0-9]+[-a-zA-Z0-9]+[^-a-zA-Z0-9]+/))
            jsonPath('$.key', byRegex(/[-._a-zA-Z0-9]+/))
            jsonPath('$.value', byRegex(nonEmpty().asString()))

        }
    }
    response {
        status BAD_REQUEST()
        body (file("errorResponses/invalidCluster.json"))
        headers {
            contentType(applicationJson())
        }
    }
    priority 1
}
