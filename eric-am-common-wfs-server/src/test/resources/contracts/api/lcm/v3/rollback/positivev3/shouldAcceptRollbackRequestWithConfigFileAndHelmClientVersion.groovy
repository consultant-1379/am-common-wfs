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
package contracts.api.lcm.v3.rollback.positivev3

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    priority 1
    description("""
Represents a successful scenario of rollback of an Application

```
given:
  client requests to rollback a resource
when:
  request is made to rollback the resource using a valid cluster config file and with valid release name
then:
  the request is accepted
```

"""
    )
    request {
        method 'POST'
        url "/api/lcm/v3/resources/${value(consumer(regex(/[a-z]([-a-z0-9]*[a-z0-9])?/)))}/rollback"
        multipart(
                json: $(c(regex(nonEmpty())), p(file('multipart-json-part-with-helm-client-version.json'))),
                clusterConfig: named(
                        name: $(c(regex(nonEmpty())), p('cluster01.config')),
                        content: $(c(regex(nonEmpty())), p(file('cluster01.config'))),
                        contentType: $(c(regex(nonEmpty())), p('text/yaml')))
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

                    "links": {
                        "self": "http://localhost/${fromRequest().path(0)}/${fromRequest().path(1)}/${fromRequest().path(2)
                }/${fromRequest().path(3)}/${fromRequest().path(4)}",

                        "instance": "http://localhost/${fromRequest().path(0)}/${fromRequest().path(1)}/${fromRequest().path(2)
                }/${fromRequest().path(3)}/${fromRequest().path(4)}?instanceId=4d2cf935-3b56-11e9-be54-02d5f77aae63_dummy_id"
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
}
