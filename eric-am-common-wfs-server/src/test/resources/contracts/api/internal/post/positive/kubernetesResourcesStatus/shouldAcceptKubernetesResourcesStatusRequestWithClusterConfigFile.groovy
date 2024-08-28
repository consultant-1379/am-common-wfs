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
package contracts.api.internal.post.positive.kubernetesResourcesStatus

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario of requesting the pods, deployments 
and stateful sets status of a resource using provided cluster config file.

```
given:
  client requests the kubernetes resources status of a specified release name
when:
  valid request is made with valid release name
then:
  the status of pods deployments and stateful sets of the specified resource 
  is returned
```

""")
    request {
        method POST()
        urlPath($(regex("/api/internal/additionalResourceInfo"))) {
            queryParameters {
                parameter 'clusterName': value(consumer(matching("[a-zA-Z0-9][-a-zA-Z0-9]+")), producer("default"))
            }
        }
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
        status OK()
        body (file("validKubernetesResourcesStatusResponseList.json"))
        headers {
            contentType(applicationJson())
        }
    }
    priority(2)
}
