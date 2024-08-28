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
package contracts.api.internal.put.positive.patchSecret

import org.springframework.cloud.contract.spec.Contract

Contract.make {
  description("""
Represents a successful scenario of patching a secret

```
given:
  client requests to patch a secret
when:
  valid patch secret request is submitted
then:
  the request is accepted
```

""")
  request {
    method 'PUT'
    url "/api/internal/kubernetes/secrets/${value(consumer(regex(/[a-z]([-a-z0-9]*[a-z0-9])?/)))}"
    headers {
      contentType(applicationJson())
    }
    body(
            "namespace": "test-namespace-4",
            "clusterName": "default",
            "key": "secret-key",
            "value": "test value for the secret key"
    )
    bodyMatchers {
      jsonPath('$.namespace', byRegex(/[a-z0-9]([-a-z0-9]*[a-z0-9])?/))
      jsonPath('$.clusterName', byRegex(/[a-zA-Z0-9][-a-zA-Z0-9]+/))
      jsonPath('$.key', byRegex(/[-._a-zA-Z0-9]+/))
      jsonPath('$.value', byRegex(nonEmpty().asString()))
    }
  }
  response {
    status ACCEPTED()
  }
  priority 2
}
