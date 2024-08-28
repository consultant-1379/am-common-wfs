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
package contracts.api.lcm.v3.get.releasehistory.negative

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents an unsuccessful scenario of requesting the release history of a resource

```
given:
  client requests the release history of a resource
when:
  request is made with a valid release name and invalid instanceId
then:
  an error message is returned
```

""")
    request {
        method 'GET'
        url "${value(consumer(regex(/\/api\/lcm\/v3\/resources\/[a-z]([-a-z0-9]*[a-z0-9])?\?instanceId=[a-z0-9]{8}(-[a-z0-9]{4}){3}-[a-z0-9]{12}(--[a-zA-Z]*)*/)),producer("/api/lcm/v3/resources/my-release?instanceId=04a3761b-36a5-11e9-a5ac-96b34b4a5326--dummyId"))}"
    }
    response {
        status NOT_FOUND()
        body(file("resourceNotFoundResponse.json"))
        headers {
            contentType(applicationJson())
        }
    }
    priority 6
}
