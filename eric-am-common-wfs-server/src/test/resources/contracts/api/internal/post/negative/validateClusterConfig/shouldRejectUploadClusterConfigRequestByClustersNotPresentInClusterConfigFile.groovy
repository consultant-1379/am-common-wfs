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
package contracts.api.internal.post.negative.validateClusterConfig


import org.springframework.cloud.contract.spec.Contract
import org.springframework.cloud.contract.spec.internal.RegexProperty

Contract.make {
    description("""
Represents an unsuccessful scenario of validating a cluster config file

```
given:
  client requests to validate cluster config file
when:
  cluster details not provided in cluster config file
then:
  the request is rejected
```

""")
    request {
        method 'POST'
        url '/api/internal/cluster/validate'
        multipart(
                clusterConfig: named(
                        name: $(consumer(regex('.+nullClusters\\.config')),
                                producer('cluster01nullClusters.config')),
                        content: $(consumer(regex(nonEmpty())),
                                producer(file('withoutClusters.config')))
                )
        )
        headers {
            contentType(multipartFormData())
        }
    }
    response {
        status BAD_REQUEST()
        body("""
            {
                "errorDetails":[
                 {
                    "parameterName":"clusters",
                    "message":"kube config clusters cannot be null"
                 }
            ]
        }
        """
        )
    }
    priority 1
}
