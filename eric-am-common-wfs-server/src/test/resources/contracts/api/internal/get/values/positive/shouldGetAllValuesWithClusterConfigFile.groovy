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
package contracts.api.internal.get.values.positive

import org.springframework.cloud.contract.spec.Contract

Contract.make {
  description("""
Represents a successful scenario of getting values in a release

```
given:
  client requests to get values in a release
when:
  valid get request is submitted with cluster config
then:
  the response with values is returned
```

""")
  request {
    method 'POST'
    urlPath($(regex('/api/internal/kubernetes/values/[a-z]([-a-z0-9]*[a-z0-9])?' ))) {
      queryParameters {
        parameter 'clusterName': value(consumer(matching("[0-9a-zA-Z][0-9a-zA-Z-_]*(\\.config)?\$")), producer("hahn131"))
        parameter 'namespace': value(consumer(regex(nonEmpty())), producer("default"))
        parameter 'fetchTimeOut': value(consumer(regex(nonEmpty())), producer("8"))
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
    status OK()
    body (file("allValuesInRelease.json"))
    headers {
      contentType(applicationJson())
    }
  }
  priority 2
}
