# E-VNFM Internal APIs 

## Overview
README to document internal APIs


### Get the Status of Pods in a Cluster by Release Name
To retrieve the status of the pods in a particular cluster create a GET request to:

'/api/internal/{releaseName}/pods?clusterName={clusterName}'

This requires no request body and retrieves the status of the pods in the cluster.

### Example curl command
```
curl -X GET http://localhost:8080/api/internal/my-release/pods?clusterName=mycluster
```
