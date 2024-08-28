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
Represents an unsuccessful scenario of instantiating an Application with a CRD resource

```
given:
  client requests to instantiate a CRD resource
when:
  an invalid instantiate request submitted with chartType = CRD
then:
  the request is rejected
```

""")
  request {
    method 'POST'
    url "/api/lcm/v3/resources/${value(consumer(regex(/(?![a-z]([-a-z0-9]*[a-z0-9])?)[^;,\/?:@&=+\|\^\[\]`]*/)), producer("2-InvalidRelease-name"))}/instantiate"
    body(
            "chartUrl": "http://arm.rnd.ki.sw.ericsson" +
                    ".se/artifactory/proj-adp-notification-service-helm/eric-un-notification-service/eric-un-notification-service-0.0.1-222.tgz",
            "lifecycleOperationId": "my-id",
            "state": "starting",
            "chartVersion": "1.2.3",
            "chartType": "CRD"
    )
    headers {
      contentType(applicationJson())
      header("Idempotency-key", $(regex(nonEmpty())))
    }
    bodyMatchers {
      jsonPath('$.chartUrl', byRegex(url()))
      jsonPath('$.lifecycleOperationId', byRegex(nonEmpty()).asString())
      jsonPath('$.state', byRegex(nonEmpty()).asString())
      jsonPath('$.chartVersion', byRegex(nonEmpty()).asString())
      jsonPath('$.chartType', byRegex("CRD|CNF"))
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
""")
    headers {
      contentType(applicationJson())
    }
  }
  priority 1
}
