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
package contracts.api.internal.post.negative.kubernetesResourcesStatus

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description(""" 
Represents a unsuccessful scenario of requesting the kubernetes resources status
 of a release

```
given:
  client requests the kubernetes resources status of a specified release
when:
  request is made with valid release name and invalid cluster name
then:
  an error message is returned
```

""")
    request {
        method POST()
        urlPath($(regex("/api/internal/additionalResourceInfo")))
        headers {
            contentType(multipartFormData())
        }
        multipart(
                json: $(c(regex(nonEmpty())), p("[\"b-rel4\"]")),
                clusterConfig: named(
                        name: $(c(regex(nonEmpty())), p('cluster01.config')),
                        content: $(consumer(nonEmpty()), producer(file('cluster01.config').asString()))
                )
        )
    }
    response {
        status BAD_REQUEST()
        body (file("clusterNameNotGivenResponse.json"))
        headers {
            contentType(applicationJson())
        }
    }
    priority(3)
}
