package contracts.api.lcm.v3.upgrade.negative

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a error scenario of upgrading an application without a config file

```
given:
  client requests to upgrade a resource without files and with multipart/from-data 
  consumed type
when:
  an invalid upgrade request submitted with invalid release name in url
then:
  the request is rejected
```

""")
    request {
        method 'POST'
        url "/api/lcm/v3/resources/${value(consumer(regex(/[0-9a-z\-]*[A-Z]+[0-9A-Za-z\-].*/)),producer("Invalid-release"))}/upgrade"
        multipart(
                json: $(c(regex(nonEmpty())), p(file('multipart-json-part-with-additional-params.json'))),
                clusterConfig: named(
                        name: $(c(regex(nonEmpty())), p('cluster01.config')),
                        content: $(c(regex(nonEmpty())), p(file('cluster01.config'))),
                        contentType: $(c(regex(nonEmpty())), p('text/yaml')))
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
                                "parameterName": "releaseName",
                                "message": "releaseName must consist of lower case alphanumeric characters or -. It must start with an alphabetic character, and end with an alphanumeric character"
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