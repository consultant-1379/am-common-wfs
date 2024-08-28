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

package contracts.api.lcm.v3.scale.negative

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a error scenario of scaling an application without a config file

```
given:
  client requests to scale a resource without files and with multipart/from-data 
  consumed type
when:
  an invalid scale request submitted
then:
  the request is rejected
```

""")
    request {
        method 'POST'
        url "/api/lcm/v3/resources/${value(consumer(regex(/[a-z]([-a-z0-9]*[a-z0-9])?/)))}/scale"
        multipart(
                json: $(c(regex(nonEmpty())), p(file('multipart-json-part.json'))),
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
                                   "parameterName": "values",
                                   "message": "Required request part 'values' is not present"
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
