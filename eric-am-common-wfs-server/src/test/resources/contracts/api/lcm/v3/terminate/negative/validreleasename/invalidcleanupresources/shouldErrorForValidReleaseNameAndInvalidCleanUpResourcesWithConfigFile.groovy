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
package contracts.api.lcm.v3.terminate.negative.validreleasename.invalidcleanupresources

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents an error scenario of terminating an Application with cluster config file

```
given:
  client requests to terminate a resource
when:
  the release name is valid and the cleanUpResources query parameter is invalid
then:
  the request is rejected
```

"""
    )
    request {
        method 'POST'
        urlPath($(regex("/api/lcm/v3/resources/[a-z]([-a-z0-9]*[a-z0-9])?/terminate"))) {
            queryParameters {
                parameter 'cleanUpResources': 'test'
            }
        }
        multipart(
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
        status BAD_REQUEST()
    }
    priority(20)
}
