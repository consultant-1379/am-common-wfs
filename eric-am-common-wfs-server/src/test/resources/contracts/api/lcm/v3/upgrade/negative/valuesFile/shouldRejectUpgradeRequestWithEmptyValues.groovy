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
package contracts.api.lcm.v3.upgrade.negative.valuesFile

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents an unsuccessful scenario of upgrading an application with an empty values.yaml file

```
given:
  client requests to upgrade a resource
when:
  an upgrade request with an empty values.yaml file is submitted
then:
  the request is rejected
```

""")
    request {
        method 'POST'
        url "/api/lcm/v3/resources/${value(consumer(regex(/[a-z]([-a-z0-9]*[a-z0-9])?/)))}/upgrade"
        multipart(
                json: $(c(regex(nonEmpty())), p('{"namespace" : "empty-values", ' +
                        '"chartName": "adp-am/my-chart", "chartVersion": "1.2.3", "lifecycleOperationId": "my-id", "state": "starting"}')),
                values: named(
                        name: $(c(regex('[A-Za-z0-9-_]*empty[.A-Za-z0-9-_]*')), p('empty_values.yaml')),
                        content: $(consumer(nonEmpty()), producer(file('empty_values.yaml.properties').asString()))
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
                errorDetails: ["message":"Unable to parse yaml file due to [Empty file], Please provide a valid yaml"]
        )
        headers {
            contentType(applicationJson())
        }
    }
    priority 7
}
