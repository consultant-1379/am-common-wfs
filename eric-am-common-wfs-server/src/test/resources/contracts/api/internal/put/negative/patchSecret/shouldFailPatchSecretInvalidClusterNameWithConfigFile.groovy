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
  client requests to patch a secret with cluster config file
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
            contentType(multipartFormData())
        }
        multipart(
                json: $(c(regex(file('jsonBodyRegex/invalid-cluster-name.txt').asString())), p(file('requestBody/invalid-cluster-name.json'))),
                clusterConfig: named(
                        name: $(c(regex(nonEmpty())), p('cluster01.config')),
                        content: $(consumer(nonEmpty()), producer(file('cluster01.config').asString()))
                )
        )
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
