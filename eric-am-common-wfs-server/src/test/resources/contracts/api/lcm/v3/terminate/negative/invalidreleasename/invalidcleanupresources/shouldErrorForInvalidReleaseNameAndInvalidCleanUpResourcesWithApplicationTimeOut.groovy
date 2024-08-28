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
package contracts.api.lcm.v3.terminate.negative.invalidreleasename.invalidcleanupresources

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents an error scenario of terminating an Application

```
given:
  client requests to terminate a resource
when:
  the release name and the cleanUpResources query parameter are invalid with a valid applicationTimeOut query parameter
then:
  the request is rejected
```

"""
    )
    request {
        method 'POST'
        urlPath($(regex("/api/lcm/v3/resources/[a-z0-9]*([-a-z-A-Z-0-9]*[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([-a-zA-Z0-9]*[a-zA-Z0-9])?)*[A-Z]+([-a-z-A-Z-0-9]*[a-zA-Z0-9])*/terminate"))){
            queryParameters {
                parameter 'cleanUpResources': 'test'
                parameter 'applicationTimeout': value(consumer(regex("[0-9]+")),producer(50))
                parameter 'lifecycleOperationId': value(consumer(regex(nonEmpty())), producer("my-id"))
                parameter 'state': value(consumer(regex(nonEmpty())), producer("starting"))
            }
        }
        headers {
            header("Idempotency-key", $(regex(nonEmpty())))
        }
    }
    response {
        status BAD_REQUEST()
    }
    priority(7)
}
