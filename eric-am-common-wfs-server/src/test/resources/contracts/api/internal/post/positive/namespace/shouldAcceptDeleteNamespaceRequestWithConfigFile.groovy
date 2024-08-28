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
package contracts.api.internal.post.positive.namespace

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario of requesting to delete the namespace of a resource
using provided cluster config file.

```
given:
  client requests to delete the namespace of a specified resource
when:
  valid request is made with valid namespace and cluster config file
then:
  202 is returned
```

""")
    request {
        method POST()
        urlPath($(regex("/api/internal/v2/namespaces/[a-z]([-a-z0-9]*[a-z0-9])?/delete"))) {
            queryParameters {
                parameter 'clusterName': value(consumer(matching("[a-zA-Z0-9][-a-zA-Z0-9]+")), producer("default"))
                parameter 'lifecycleOperationId': value(consumer(matching("[a-zA-Z0-9][-a-zA-Z0-9]+")), producer("lifecycle-id"))
                parameter 'applicationTimeOut': value(consumer(matching("[a-zA-Z0-9][-a-zA-Z0-9]+")), producer("3000"))
                parameter 'releaseName': value(consumer(matching("[a-zA-Z0-9][-a-zA-Z0-9]+")), producer("release"))
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
        status ACCEPTED()
    }
    priority(2)
}

