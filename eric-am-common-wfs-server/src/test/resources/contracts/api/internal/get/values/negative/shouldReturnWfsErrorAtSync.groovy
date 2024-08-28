/*******************************************************************************
 * COPYRIGHT Ericsson 2024
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
package contracts.api.internal.get.values.negative

import org.springframework.cloud.contract.spec.Contract

[
        Contract.make {
            description("""
Represents a failure scenario for Sync e2e test when WFS responds with 5xx error

```
given:
  client requests to get values in a sync
when:
  invalid post request is submitted
then:
  fails with internal server error;

```

""")
            request {
                method 'POST'
                urlPath($(regex('/api/internal/kubernetes/values/end-to-end-sync-wfs-error-1'))) {
                    queryParameters {
                        parameter 'namespace': value(consumer(regex(nonEmpty())), producer("default"))
                        parameter 'clusterName': value(consumer(regex(nonEmpty())), producer("myCluster"))
                    }
                }
                headers {
                    contentType(multipartFormData())
                }
            }
            response {
                status INTERNAL_SERVER_ERROR()
                body (file("errorDuringCommandProcessing.json"))
                headers {
                    contentType(applicationJson())
                }
            }
            priority 1
        }
]
