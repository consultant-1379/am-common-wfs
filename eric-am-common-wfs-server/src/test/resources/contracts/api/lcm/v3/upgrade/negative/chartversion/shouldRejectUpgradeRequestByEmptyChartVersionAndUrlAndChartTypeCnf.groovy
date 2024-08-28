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
package contracts.api.lcm.v3.upgrade.negative.chartversion

import org.springframework.cloud.contract.spec.Contract

Contract.make {
  description("""
Represents an unsuccessful scenario of upgrading an Application with a CNF resource

```
given:
client requests to upgrade a resource

when:
an invalid upgrade request submitted both chartVersion and chartUrl are in the request body, chartVersion is also empty and chartType = CNF

then:
 the request is rejected
  ```

""")
  request {
    method 'POST'
    url "/api/lcm/v3/resources/${value(consumer(regex(/[a-z]([-a-z0-9]*[a-z0-9])?/)))}/upgrade"
    body(
            "chartVersion": "",
            "chartUrl": "https://arm.rnd.ki.sw.ericsson.se/artifactory/proj-adp-spider-team-helm/spider-app/spider-app-2.74.8.tgz",
            "lifecycleOperationId": "my-id",
            "state": "starting",
            "chartType": "CNF"
    )
    headers {
      contentType(applicationJson())
      header("Idempotency-key", $(regex(nonEmpty())))
    }
    bodyMatchers {
      jsonPath('$.chartUrl', byRegex(url()).asString())
      jsonPath('$.lifecycleOperationId', byRegex(nonEmpty()).asString())
      jsonPath('$.state', byRegex(nonEmpty()).asString())
      jsonPath('$.chartType', byRegex("CRD|CNF"))
    }
  }
    response {
      status BAD_REQUEST()

      body("""
              {
    "errorDetails": [
        {
            "parameterName": "chartName, chartUrl, chartVersion",
            "message": "If chartUrl property has been specified, chartName and chartVersion properties should not be set. Please see API documentation for correct usage."
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
