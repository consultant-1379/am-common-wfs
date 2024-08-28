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
Represents an unsuccessful scenario of validating a cluster config file where the connection test fails.
There are a number of reasons for the connection test to fail:
1. The IP address is wrong
2. There is no connectivity to the cluster
3. The cluster is down
4. The user credentials are wrong
5. The Kubernetes & Helm versions between the two clusters are incompatible
6. The connection to the cluster is slow & the command times out.

```
given:
  client requests to validate cluster config file
when:
  the connection test fails
then:
  the request is rejected
```

""")
    request {
        method 'POST'
        url '/api/internal/cluster/validate'
        multipart(
                clusterConfig: named(
                        name: $(consumer('testConnectionFails.config'),
                                producer('testConnectionFails.config')),
                        content: $(consumer(regex(nonEmpty())),
                                producer(file('testConnectionFails.config')))
                )
        )
        headers {
            contentType(multipartFormData())
        }
    }
    response {
        status BAD_REQUEST()
        bodyMatchers {
            jsonPath('$.errorDetails', byRegex("Connectivity test failed due to .*"))
        }
    }
    priority 1
}
