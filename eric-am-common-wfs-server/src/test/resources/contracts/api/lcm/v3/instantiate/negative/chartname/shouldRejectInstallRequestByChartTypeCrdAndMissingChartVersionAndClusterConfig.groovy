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
package contracts.api.lcm.v3.instantiate.negative.chartname

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents an unsuccessful scenario of instantiating an Application with a CRD resource

```
given:
  client requests to instantiate a CRD resource
when:
  invalid instantiate request submitted with chartType = CRD and missing chartVersion 
then:
  the request is rejected
```

""")
    request {
        method 'POST'
        url "/api/lcm/v3/resources/${value(consumer(regex(/[a-z]([-a-z0-9]*[a-z0-9])?/)))}/instantiate"
        headers {
            contentType(multipartFormData())
            header("Idempotency-key", $(regex(nonEmpty())))
        }
        multipart(
                json: $(c(regex(file('crd-no-chart-version-request-pattern.txt').asString())),  p(file('crd-no-chart-version-request-body.json'))),
                clusterConfig: named(
                        name: $(c(regex(nonEmpty())), p('cluster01.config')),
                        content: $(c(regex(nonEmpty())), p(file('cluster01.config'))))
        )
    }
    response {
        status BAD_REQUEST()
        body("""
              {
    "errorDetails": [
        {
            "parameterName": "chartVersion",
            "message": "chartVersion is required for CRD chartType"
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
