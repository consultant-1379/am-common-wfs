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
package contracts.api.lcm.v3.terminate.positivev3

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario of terminating an Application

```
given:
  client requests to terminate a resource
when:
  the release name is valid with a valid applicationTimeOut query parameter
then:
  the request is accepted
```

"""
    )
    request {
        method 'POST'
        urlPath($(regex("/api/lcm/v3/resources/[a-z]([-a-z0-9]*[a-z0-9])?/terminate"))) {
            queryParameters {
                parameter 'applicationTimeout': value(consumer(regex(/([0-9]+)/)),producer("50"))
                parameter 'lifecycleOperationId': value(consumer(regex(nonEmpty())), producer("my-id"))
                parameter 'state': value(consumer(regex(nonEmpty())), producer("starting"))
                parameter 'namespace': value(consumer(regex(nonEmpty())), producer("default"))
            }
        }
        headers {
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
                    "self": "http://localhost/${fromRequest().path(0)}/${fromRequest().path(1)}/${fromRequest().path(2)}/${fromRequest().path(3)}/${fromRequest().path(4)}",
                    "instance": "http://localhost/${fromRequest().path(0)}/${fromRequest().path(1)}/${fromRequest().path(2)}/${fromRequest().path(3)}/${fromRequest().path(4)}?instanceId=4d2cf935-3b56-11e9-be54-02d5f77aae63_dummy_id"
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
    priority(24)
}
