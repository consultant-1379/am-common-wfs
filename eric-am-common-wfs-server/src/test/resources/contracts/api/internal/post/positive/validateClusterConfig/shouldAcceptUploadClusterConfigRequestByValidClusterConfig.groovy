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
package contracts.api.internal.post.positive.validateClusterConfig


import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario of validating a cluster config file

```
given:
  client requests to validate cluster config file
when:
  valid cluster config file is provided
then:
  the request is accepted
```

""")
    request {
        method 'POST'
        url '/api/internal/cluster/validate'
        multipart(
                clusterConfig: named(
                        name: $(consumer(regex('[a-zA-Z\\d_-]+\\.config')),
                                producer('cluster01.config')),
                        content: $(consumer(nonEmpty()),
                                producer(file('cluster01.config')))
                )
        )
        headers {
            contentType(multipartFormData())
        }
    }
    response {
        status OK()
        body(file("clusterServerDetailsResponse.json"))
        headers {
            contentType(applicationJson())
        }
    }
    priority 2
}
