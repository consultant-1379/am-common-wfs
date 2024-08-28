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
package contracts.api.internal.get.getAllSecrets.positive

import org.springframework.cloud.contract.spec.Contract

Contract.make {
  description("""
Represents a successful scenario of getting all secret in a namespace

```
given:
  client requests to get all secret in a namespace
when:
  valid get request is submitted
then:
  the response with all secret details is returned
```

""")
  request {
    method 'GET'
    urlPath('/api/internal/kubernetes/secrets' ) {
      queryParameters {
        parameter 'clusterName': value(consumer(matching("[0-9a-zA-Z][0-9a-zA-Z-_]*(\\.config)?\$")), producer("default"))
        parameter 'fetchTimeOut': value(consumer(regex(nonEmpty())), producer("32"))
        parameter 'namespace': value(consumer(regex(nonEmpty())), producer("default"))
      }
    }
    headers {
      contentType(applicationJson())
    }
  }
  response {
    status OK()
    body (file("validGetAllSecret.json"))
    headers {
      contentType(applicationJson())
    }
  }
  priority 2
}
