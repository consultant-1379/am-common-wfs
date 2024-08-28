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
package contracts.api.internal.post.negative.pvcs

import org.springframework.cloud.contract.spec.Contract

Contract.make {
  description("""
Represents a failed scenario of deleting pvcs with with missing clusterName with cluster config file

```
given:
  client requests to delete pvcs
when:
  the release name is valid and the clusterName query parameter is invalid
then:
  the request is rejected
```

"""
  )
  request {
    method 'POST'
    urlPath($(regex("/api/internal/kubernetes/pvcs/[a-z]([-a-z0-9]*[a-z0-9])?/delete"))) {
      queryParameters {
        parameter 'clusterName': value(consumer(regex("[a-zA-Z0-9-_]+\\+[a-zA-Z0-9-._]+")),producer("my+cluster"))
        parameter 'lifecycleOperationId': value(consumer(regex(nonEmpty())), producer("my-id"))
        parameter 'state': value(consumer(regex(nonEmpty())), producer("starting"))
        parameter 'namespace': value(consumer(regex(nonEmpty())), producer("default"))
      }
    }
    multipart(
            clusterConfig: named(
                    name: $(c(regex(nonEmpty())), p('cluster01.config')),
                    content: $(consumer(nonEmpty()), producer(file('clusterconfig/cluster01.config').asString()))
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
            """
                {
                "errorDetails": [
                      {
                        "parameterName": "clusterName",
                        "message": "clusterName must consist of alphanumeric characters. It can be given as just the name or ending with .config"
                      }
                    ]
                }
            """

    )
  }
  priority(1)
}
