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
Represents a successful scenario of instantiating an Application

```
given:
  client requests to instantiate a resource
when:
  valid instantiate request submitted with a valid cluster name 
then:
  the request is accepted
```

""")
  request {
    method 'POST'
    url "/api/lcm/v3/resources/${value(consumer(regex(/[a-z]([-a-z0-9]*[a-z0-9])?/)))}/instantiate"
    body(
            "chartUrl": "http://arm.rnd.ki.sw.ericsson.se/artifactory/proj-adp-notification-service-helm/eric-un-notification-service/eric-un-notification-service-0.0.1-222.tgz",
            "clusterName": "mycluster",
            "lifecycleOperationId": "my-id",
            "state": "starting"
    )
    headers {
      contentType(applicationJson())
      header("Idempotency-key", $(regex(nonEmpty())))
    }
    bodyMatchers {
      jsonPath('$.chartUrl', byRegex(url()).asString())
      jsonPath('$.clusterName', byRegex('[a-zA-Z0-9]+(\\.config)?'))
      jsonPath('$.lifecycleOperationId', byRegex(nonEmpty()).asString())
      jsonPath('$.state', byRegex(nonEmpty()).asString())
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
