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

package contracts.api.lcm.v3.scale.positivev3

import org.springframework.cloud.contract.spec.Contract

Contract.make {
  description("""
Represents a successful scenario of scaling an application with a values.yaml file and individual parameters
```
given:
  client requests to scale a resource with a values.yaml file and individual parameters
when:
  valid scale request submitted
then:
  the request is accepted
```

""")
  request {
    method 'POST'
    url "/api/lcm/v3/resources/${value(consumer(regex(/[a-z]([-a-z0-9]*[a-z0-9])?/)))}/scale"
    multipart(
            json: $(c(regex(nonEmpty())), p(file('request-with-additional-params.json'))),
            values: named(
                    name: $(c(regex(nonEmpty())), p('values.yaml')),
                    // File extension couldn't be yaml as then it was processed as a contract
                    content: $(consumer(nonEmpty()), producer(file('values.yaml.properties')))
            )
    )
    headers {
      contentType(multipartFormData())
      header("Idempotency-key", $(regex(nonEmpty())))
    }
  }
  response {
    status ACCEPTED()
    body(
            releaseName: "${fromRequest().path(4)}",
            instanceId: "4d2cf935-3b56-11e9-be54-02d5f77aae63_dummy_id"
    )
    bodyMatchers {
      jsonPath('$.releaseName', byRegex(/[a-z]([-a-z0-9]*[a-z0-9])?/))
    }
    headers {
      contentType(applicationJson())
    }
  }
  priority(1)
}
