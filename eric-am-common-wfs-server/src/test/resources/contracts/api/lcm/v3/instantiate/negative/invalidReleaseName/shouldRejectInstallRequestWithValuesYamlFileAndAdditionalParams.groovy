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
Represents an unsuccessful scenario of instantiating an application with a values.yaml file and individual parameters

```
given:
  client requests to instantiate a resource with a values.yaml file and individual parameters
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
            json: $(c(regex('([a-zA-Z0-9_-]*(additional-params){1}[a-zA-Z0-9.]*)')), p(file('multipart-json-part-with-additional-params.json'))),
            values: named(
                    name: $(c(regex(nonEmpty())), p('values.yaml')),
                    // content of the file
                    content: $(c(regex(nonEmpty())), p(file('values.yaml.properties'))),
                    // content type for the part
                    contentType: $(c(regex(nonEmpty())), p('application/octet-stream')))
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
  priority 8
}
