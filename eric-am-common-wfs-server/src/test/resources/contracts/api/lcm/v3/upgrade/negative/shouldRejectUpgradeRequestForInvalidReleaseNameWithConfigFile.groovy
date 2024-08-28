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
package contracts.api.lcm.v3.upgrade.negative

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a error scenario of upgrading an application without a config file

```
given:
  client requests to upgrade a resource without files and with multipart/from-data 
  consumed type
when:
  an invalid upgrade request submitted with invalid release name in json
then:
  the request is rejected
```

""")
    request {
        method 'POST'
        url "/api/lcm/v3/resources/${value(consumer(regex(/[a-z0-9]*([-a-z-A-Z-0-9]*[a-zA-Z0-9])?(\.[a-zA-Z0-9]([-a-zA-Z0-9]*[a-zA-Z0-9])?)*[A-Z]+([-a-z-A-Z-0-9]*[a-zA-Z0-9])*/)))}/upgrade"
        multipart(
                json: $(c(regex('.+"releaseName":"[0-9a-z\\-]*[A-Z]+[0-9A-Za-z\\-].*".+')), p(file('multipart-json-part-with-additional-params.json'))),
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
        status BAD_REQUEST()
        body(
                """
                                     {
                           "errorDetails": [
                               {
                                "parameterName": "releaseName",
                                "message": "releaseName must consist of lower case alphanumeric characters or -. It must start with an alphabetic character, and end with an alphanumeric character"
                               }
                           ]
                       }
                 """
        )
        headers {
            contentType(applicationJson())
        }
    }
}
