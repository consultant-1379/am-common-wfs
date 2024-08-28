/*******************************************************************************
 * COPYRIGHT Ericsson 2024
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
package contracts.api.lcm.v3.instantiate.positive.v3

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents an unsuccessful scenario of instantiating an application with a cluster01.config file

```
given:
  client requests to instantiate a resource with a cluster01.config file
when:
  an invalid instantiate request submitted
then:
  the request is rejected
```

""")
    request {
        method 'POST'
        url "/api/lcm/v3/resources/${value(consumer(regex(/(?![a-z]([-a-z0-9]*[a-z0-9])?)[^;,\/?:@&=+\|\^\[\]`]*/)), producer("2-InvalidRelease-name"))}/instantiate"
        multipart(
                json: $(c(regex(nonEmpty())), p(file('multipart-json-part-with-helm-client-version.json'))),
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

        body("""
              {
    "errorDetails": [
        {
           "parameterName": "releaseName",
            "message": "releaseName must consist of lower case alphanumeric characters or -. It must start with an alphabetic character, and end with an alphanumeric character"
        }
    ]
}
"""
        )
        headers {
            contentType(applicationJson())
        }
    }
    priority 3
}
