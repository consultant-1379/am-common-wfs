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
  client requests to patch a secret with cluster config file
when:
  valid patch secret request is submitted without a cluster name and empty secret value
then:
  the request is accepted
```

""")
    request {
        method 'PUT'
        url "/api/internal/kubernetes/secrets/${value(consumer(regex(/[a-z]([-a-z0-9]*[a-z0-9])?/)))}"
        headers {
            contentType(multipartFormData())
        }
        multipart(
                json: $(c(regex(file('jsonBodyRegex/no-cluster-name-and-secret.txt').asString())), p(file('requestBody/no-cluster-name-and-secret.json'))),
                clusterConfig: named(
                        name: $(c(regex(nonEmpty())), p('cluster01.config')),
                        content: $(consumer(nonEmpty()), producer(file('cluster01.config').asString()))
                )
        )
    }
    response {
        status ACCEPTED()
    }
    priority 2
}
