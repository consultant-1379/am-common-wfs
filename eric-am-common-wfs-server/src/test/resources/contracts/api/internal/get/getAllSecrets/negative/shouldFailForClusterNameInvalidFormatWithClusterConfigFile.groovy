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
package contracts.api.internal.get.getAllSecrets.negative

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a failure scenario of getting all secret in a namespace
with valid cluster config file provided

```
given:
  client requests to get all secret in a namespace
when:
  invalid get request is submitted with empty namespace
then:
  the error response is returned
```

""")
    request {
        method 'POST'
        urlPath('/api/internal/kubernetes/secrets' ) {
            queryParameters {
                parameter 'clusterName': value(consumer(matching("[^A-Za-z0-9]")), producer("default£"))
                parameter 'namespace': value(consumer(regex(nonEmpty())), producer("default"))
            }
        }
        headers {
            contentType(multipartFormData())
        }
        multipart(
                clusterConfig: named(
                        name: $(c(regex(nonEmpty())), p('cluster01.config')),
                        content: $(c(nonEmpty()), p(file('cluster01.config').asString()))
                )
        )
    }
    response {
        status BAD_REQUEST()
        body (file("clusterNameInvalidErrorMessage.json"))
        headers {
            contentType(applicationJson())
        }
    }
    priority 1
}
