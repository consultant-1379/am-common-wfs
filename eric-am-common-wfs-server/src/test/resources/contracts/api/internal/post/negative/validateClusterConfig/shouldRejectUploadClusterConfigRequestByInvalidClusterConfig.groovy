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

Contract.make {
    description("""
Represents an unsuccessful scenario of validating a cluster config file

```
given:
  client requests to validate cluster config file
when:
  invalid cluster config file is provided
then:
  the request is rejected
```

""")
    request {
        method 'POST'
        url '/api/internal/cluster/validate'
        multipart(
                clusterConfig: named(
                        name: $(consumer(regex('.+multiUCC.config')),
                                producer('cluster01multiUCC.config')),
                        content: $(consumer(regex(nonEmpty())),
                                producer(file('withMultipleClusterUserContext.config')))
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
         "parameterName":"users",
         "message":"only one user is allowed in kube config file"
      },
      {
         "parameterName":"clusters",
         "message":"only one cluster is allowed in kube config file"
      },
      {
         "parameterName":"contexts",
         "message":"only one context is allowed in kube config file"
      }
   ]
}
        """
        )
    }
    priority 1
}
