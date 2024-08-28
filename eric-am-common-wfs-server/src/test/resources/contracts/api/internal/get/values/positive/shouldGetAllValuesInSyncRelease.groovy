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
package contracts.api.internal.get.values.positive

import org.springframework.cloud.contract.spec.Contract

[
        Contract.make {
            description("""
Represents a successful scenario of getting values in a sync success release 1

```
given:
  client requests to get values in a sync success release 1
when:
  valid post request is submitted
then:
  the response with values is returned
```

""")
            request {
                method 'POST'
                urlPath($(regex('/api/internal/kubernetes/values/end-to-end-sync-success-1'))) {
                    queryParameters {
                        parameter 'clusterName': value(consumer(matching("[0-9a-zA-Z][0-9a-zA-Z-_]*(\\.config)?\$")), producer("default"))
                        parameter 'namespace': value(consumer(regex(nonEmpty())), producer("default"))
                    }
                }
                headers {
                    contentType(multipartFormData())
                }
            }
            response {
                status OK()
                body(file("allValuesInSyncSuccessReleaseChart1.json"))
                headers {
                    contentType(applicationJson())
                }
            }
            priority 1
        },
        Contract.make {
            description("""
Represents a successful scenario of getting values in a sync success release 2

```
given:
  client requests to get values in a sync success release 2
when:
  valid post request is submitted
then:
  the response with values is returned
```

""")
            request {
                method 'POST'
                urlPath($(regex('/api/internal/kubernetes/values/end-to-end-sync-success-2'))) {
                    queryParameters {
                        parameter 'clusterName': value(consumer(matching("[0-9a-zA-Z][0-9a-zA-Z-_]*(\\.config)?\$")), producer("default"))
                        parameter 'namespace': value(consumer(regex(nonEmpty())), producer("default"))
                    }
                }
                headers {
                    contentType(multipartFormData())
                }
            }
            response {
                status OK()
                body(file("allValuesInSyncSuccessReleaseChart2.json"))
                headers {
                    contentType(applicationJson())
                }
            }
            priority 1
        },
        Contract.make {
            description("""
Represents a successful scenario of getting values in a sync failure release 1

```
given:
  client requests to get values in a sync failure release 1
when:
  valid post request is submitted
then:
  the response with values is returned
```

""")
            request {
                method 'POST'
                urlPath($(regex('/api/internal/kubernetes/values/end-to-end-sync-failure-1'))) {
                    queryParameters {
                        parameter 'clusterName': value(consumer(matching("[0-9a-zA-Z][0-9a-zA-Z-_]*(\\.config)?\$")), producer("default"))
                        parameter 'namespace': value(consumer(regex(nonEmpty())), producer("default"))
                    }
                }
                headers {
                    contentType(multipartFormData())
                }
            }
            response {
                status OK()
                body(file("allValuesInSyncFailureReleaseChart1.json"))
                headers {
                    contentType(applicationJson())
                }
            }
            priority 1
        },
        Contract.make {
            description("""
Represents a successful scenario of getting values in a sync failure release 2

```
given:
  client requests to get values in a sync failure release 2
when:
  valid post request is submitted
then:
  the response with values is returned
```

""")
            request {
                method 'POST'
                urlPath($(regex('/api/internal/kubernetes/values/end-to-end-sync-failure-2'))) {
                    queryParameters {
                        parameter 'clusterName': value(consumer(matching("[0-9a-zA-Z][0-9a-zA-Z-_]*(\\.config)?\$")), producer("default"))
                        parameter 'namespace': value(consumer(regex(nonEmpty())), producer("default"))
                    }
                }
                headers {
                    contentType(multipartFormData())
                }
            }
            response {
                status OK()
                body(file("allValuesInSyncFailureReleaseChart2.json"))
                headers {
                    contentType(applicationJson())
                }
            }
            priority 1
        }
]
