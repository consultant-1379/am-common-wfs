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
Represents an unsuccessful scenario of instantiating an application with a values.yaml file and a CRD resource

```
given:
  client requests to instantiate a resource with a values.yaml file
when:
  an invalid instantiate request submitted with a chartType = CRD
then:
  the request is rejected
```

""")
  request {
    method 'POST'
    url "/api/lcm/v3/resources/${value(consumer(regex(/(?![a-z]([-a-z0-9]*[a-z0-9])?)[^;,\/?:@&=+\|\^\[\]`]*/)), producer("2-InvalidRelease-name"))}/instantiate"
    multipart(
            json: $(c(regex(nonEmpty())), p(file('multipart-json-part-chartType.json'))),
            values: named(
                    name: $(c(regex(nonEmpty())), p('values.yaml')),
                    // File extension couldn't be yaml as then it was processed as a contract
                    content: $(consumer(nonEmpty()), producer(file('values.yaml.properties').asString()))
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
  priority 1
}
