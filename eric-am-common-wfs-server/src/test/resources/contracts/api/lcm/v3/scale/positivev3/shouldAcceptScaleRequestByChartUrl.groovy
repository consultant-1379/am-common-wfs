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
Represents a successful scenario of scaling an Application

```
given:
  client requests to perform an scale
when:
  valid scale request submitted
then:
  the request is accepted
```

""")
    request {
        method 'POST'
        url "/api/lcm/v3/resources/${value(consumer(regex(/[a-z]([-a-z0-9]*[a-z0-9])?/)))}/scale"
        body(
                """
                {
                  "chartUrl": "https://arm.rnd.ki.sw.ericsson.se/artifactory/proj-adp-spider-team-helm/spider-app/spider-app-2.74.8.tgz",
                    "lifecycleOperationId": "my-id",
            "state": "starting",
                    "namespace":"default",
                  "scaleResources": {
                      "test-deployment": {
                        "someParameter.replica": 3,
                        "someParameter.minReplica": 3,
                        "someParameter.maxReplica": 3
                      }
                  }
                }
                """

        )
        headers {
            contentType(applicationJson())
            header("Idempotency-key", $(regex(nonEmpty())))
        }
        bodyMatchers {
            jsonPath('$.chartUrl', byRegex(url()).asString())
            jsonPath('$.lifecycleOperationId', byRegex(nonEmpty()).asString())
            jsonPath('$.state', byRegex(nonEmpty()).asString())
            jsonPath('$.namespace', byRegex(nonEmpty()).asString())
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
    priority 8
}
