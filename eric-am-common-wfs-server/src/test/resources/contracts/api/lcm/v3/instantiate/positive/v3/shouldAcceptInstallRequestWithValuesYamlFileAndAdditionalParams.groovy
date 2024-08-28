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
Represents a successful scenario of instantiating an application with a values.yaml file and individual parameters

```
given:
  client requests to instantiate a resource with a values.yaml file and individual parameters
when:
  valid instantiate request submitted
then:
  the request is accepted
```

""")
  request {
    method 'POST'
    url "/api/lcm/v3/resources/${value(consumer(regex(/[a-z]([-a-z0-9]*[a-z0-9])?/)))}/instantiate"
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
  priority 8
}
