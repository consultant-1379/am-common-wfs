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
package contracts.api.internal.post.positive.scaleDown

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario of scaling down an instantiated resource
with cluster config file provided - cluster01.config

```
given:
  client requests to scale a resource down with a cluster01.config file
when:
  valid scale down request is submitted
then:
  the request is accepted
```

""")
    request {
        method 'POST'
        url '/api/internal/kubernetes/pods/scale/down'
        multipart(
                json: $(c(regex(nonEmpty())), p(file('multipart-json-part.json'))),
                clusterConfig: named(
                        name: $(c(regex(nonEmpty())), p('cluster01.config')),
                        content: $(consumer(nonEmpty()), producer(file('cluster01.config').asString()))
                )
        )
        headers {
            contentType(multipartFormData())
            header("Idempotency-key", $(regex(nonEmpty())))
        }
    }
    response {
        status ACCEPTED()
    }
    priority 3
}
