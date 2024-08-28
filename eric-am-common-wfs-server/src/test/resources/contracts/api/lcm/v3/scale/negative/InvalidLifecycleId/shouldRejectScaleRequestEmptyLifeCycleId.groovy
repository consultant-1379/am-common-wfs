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

package contracts.api.lcm.v3.scale.negative.InvalidLifecycleId

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents an unsuccessful scenario of scales of an Application

```
given:
  client requests to scale a resource
when:
  Request with empty lifecycleOperationId
then:
  the request is rejected
```

""")
    request {
        method 'POST'
        url "/api/lcm/v3/resources/${value(consumer(regex(/[a-z]([-a-z0-9]*[a-z0-9])?/)))}/scale"
        body([
                chartUrl            : "https://arm.rnd.ki.sw.ericsson.se/artifactory/proj-adp-spider-team-helm/spider-app/spider-app-2.74.8.tgz",
                lifecycleOperationId: '',
                state               : "starting",
                namespace           : "default",
                scaleResources      : [
                        'test-deployment': [
                                'someParameter.replica'   : 3,
                                'someParameter.minReplica': 3,
                                'someParameter.maxReplica': 3
                        ]
                ]
        ])
        bodyMatchers {
            jsonPath('$.chartUrl', byRegex(url()))
            jsonPath('$.lifecycleOperationId', byRegex(''))
            jsonPath('$.state', byRegex(onlyAlphaUnicode()))
            jsonPath('$.namespace', byRegex(nonEmpty()))
            jsonPath("\$.['scaleResources'].['test-deployment'].['someParameter.replica']", byRegex(number()))
            jsonPath("\$.['scaleResources'].['test-deployment'].['someParameter.minReplica']", byRegex(number()))
            jsonPath("\$.['scaleResources'].['test-deployment'].['someParameter.maxReplica']", byRegex(number()))
        }
        headers {
            contentType(applicationJson())
            header("Idempotency-key", $(regex(nonEmpty())))
        }
    }
    response {
        status BAD_REQUEST()

        body("""
              {
    "errorDetails": [
        {
            "parameterName": "lifecycleOperationId",
            "message": "lifecycleOperationId cannot be empty"
        }
    ]
}
"""
        )
        headers {
            contentType(applicationJson())
        }
    }
    priority 8
}
