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
package contracts.api.lcm.v3.instantiate.positive.v3

import org.springframework.cloud.contract.spec.Contract

Contract.make {
  description("""
Represents a successful scenario of instantiating an application with a values.yaml file and a CRD resource

```
given:
  client requests to instantiate a resource with a values.yaml file
when:
  valid instantiate request submitted with a chartType = CRD
then:
  the request is accepted
```

""")
  request {
    method 'POST'
    url "/api/lcm/v3/resources/${value(consumer(regex(/[a-z]([-a-z0-9]*[a-z0-9])?/)))}/instantiate"
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
    status ACCEPTED()
    body(
            """
                {
                    "releaseName": "${fromRequest().path(4)}",
                    "instanceId": "4d2cf935-3b56-11e9-be54-02d5f77aae63_dummy_id",
                    "links":
                    {
                      "self": "http://localhost/${fromRequest().url()}/${fromRequest().path(4)}",
                      "instance": "http://localhost/${fromRequest().url()}/${fromRequest().path(4)}?instanceId=4d2cf935-3b56-11e9-be54-02d5f77aae63_dummy_id"
                    }
                }
            """
    )
    bodyMatchers {
      jsonPath('$.releaseName', byRegex(/[a-z]([-a-z0-9]*[a-z0-9])?/))
      jsonPath('$.links', byCommand("assertThat(parsedJson.read(\"\$.links\", Object.class)).isNotNull()"))
      jsonPath('$.links.self', byRegex(/(http[s]?:\/\/)?([^\/\s]+\/)(.*)/))
      jsonPath('$.links.instance', byRegex(/(http[s]?:\/\/)?([^\/\s]+\/)(.*)/))
    }
    headers {
      contentType(applicationJson())
    }
  }
  priority 1
}
