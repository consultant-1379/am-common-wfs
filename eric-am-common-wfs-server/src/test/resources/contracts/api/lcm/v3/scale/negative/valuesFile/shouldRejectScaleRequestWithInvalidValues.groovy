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
package contracts.api.lcm.v3.scale.negative.valuesFile

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents an unsuccessful scenario of scaling an application with an invalid values.yaml file

```
given:
  client requests to scale a resource
when:
  a scale request with an invalid values.yaml file is submitted
then:
  the request is rejected
```

""")
    request {
        method 'POST'
        url "/api/lcm/v3/resources/${value(consumer(regex(/[a-z]([-a-z0-9]*[a-z0-9])?/)))}/scale"
        multipart(
                json: $(c(regex(nonEmpty())), p('{"chartName": "adp-am/my-chart", "lifecycleOperationId": "2ad705fe-be5f-4c12-a9d1-7b2368173487", ' +
                        '"state": "PROCESSING", "namespace":"default", "scaleResources": {"test-deployment": {"someParameter.replica": 3, ' +
                        '"someParameter.minReplica": 3, "someParameter.maxReplica": 3}}}')),
                values: named(
                        name: $(c(regex('[A-Za-z0-9-_]*invalid[.A-Za-z0-9-_]*')), p('invalid_values.yaml')),
                        content: $(consumer(nonEmpty()), producer(file('invalid_values.yaml.properties').asString()))
                )
        )
        headers {
            contentType(multipartFormData())
            header("Idempotency-key", $(regex(nonEmpty())))
        }
    }
    response {
        status BAD_REQUEST()
        body(
                errorDetails: ["message": "Unable to parse yaml file due to [Invalid Yaml file provided], Please provide a valid yaml"]
        )
        headers {
            contentType(applicationJson())
        }
    }
    priority 7
}
