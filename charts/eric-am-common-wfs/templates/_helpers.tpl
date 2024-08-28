{{/* vim: set filetype=mustache: */}}
{{/*
Expand the name of the chart.
*/}}
{{- define "eric-am-common-wfs.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
*/}}
{{- define "eric-am-common-wfs.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- template "eric-am-common-wfs.name" . -}}
{{- end -}}
{{- end -}}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "eric-am-common-wfs.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create main image registry url
*/}}
{{- define "eric-am-common-wfs.mainImagePath" -}}
  {{- include "eric-eo-evnfm-library-chart.mainImagePath" (dict "ctx" . "svcRegistryName" "commonWfs") -}}
{{- end -}}

{/*
The pgInitContainer image registry url
*/}}
{{- define "eric-am-common-wfs.pgInitContainerPath" -}}
  {{- include "eric-eo-evnfm-library-chart.mainImagePath" (dict "ctx" . "svcRegistryName" "pgInitContainer") -}}
{{- end -}}

{{/*
Create bro-agent-filemount image registry url
*/}}
{{- define "eric-am-common-wfs.bro-agent-filemount.registryUrl" -}}
  {{- if index .Values "imageCredentials" "bro-agent-filemount" "registry" -}}
    {{- if index .Values "imageCredentials" "bro-agent-filemount" "registry" "url" -}}
      {{- print index .Values "imageCredentials" "bro-agent-filemount" "registry" "url" -}}
    {{- end -}}
  {{- else -}}
    {{- print .Values.global.registry.url -}}
  {{- end -}}
{{- end -}}

{{/*
Create image pull secrets
*/}}
{{- define "eric-am-common-wfs.pullSecrets" -}}
  {{- include "eric-eo-evnfm-library-chart.pullSecrets" . -}}
{{- end -}}

{{/*
Create Ericsson Product Info
*/}}
{{- define "eric-am-common-wfs.helm-annotations" -}}
  {{- include "eric-eo-evnfm-library-chart.helm-annotations" . -}}
{{- end -}}

{{/*
Create prometheus info
*/}}
{{- define "eric-am-common-wfs.prometheus" -}}
  {{- include "eric-eo-evnfm-library-chart.prometheus" . -}}
{{- end -}}

{{/*
Create Ericsson product app.kubernetes.io info
*/}}
{{- define "eric-am-common-wfs.kubernetes-io-info" -}}
  {{- include "eric-eo-evnfm-library-chart.kubernetes-io-info" . -}}
{{- end -}}

{{/*
Create pullPolicy for workflow service container
*/}}
{{- define "eric-am-common-wfs.imagePullPolicy" -}}
  {{- include "eric-eo-evnfm-library-chart.imagePullPolicy" (dict "ctx" . "svcRegistryName" "commonWfs") -}}
{{- end -}}

{{/*
Create pullPolicy for workflow service bro-agent-filemount container
*/}}
{{- define "eric-am-common-wfs.bro-agent-filemount.imagePullPolicy" -}}
  {{- include "eric-eo-evnfm-library-chart.imagePullPolicy" (dict "ctx" . "svcRegistryName" "bro-agent-filemount") -}}
{{- end -}}

{{/*
Create pullPolicy for workflow service create-db-schema container
*/}}
{{- define "eric-am-common-wfs.create-db-schema.imagePullPolicy" -}}
  {{- include "eric-eo-evnfm-library-chart.imagePullPolicy" (dict "ctx" . "svcRegistryName" "create-db-schema") -}}
{{- end -}}

{{/*
The name of the cluster role used during openshift deployments.
This helper is provided to allow use of the new global.security.privilegedPolicyClusterRoleName if set, otherwise
use the previous naming convention of <release_name>-allowed-use-privileged-policy for backwards compatibility.
*/}}
{{- define "eric-am-common-wfs.privileged.cluster.role.name" -}}
  {{- include "eric-eo-evnfm-library-chart.privileged.cluster.role.name" ( dict "ctx" . "svcName" (include "eric-am-common-wfs.name" .) ) -}}
{{- end -}}

{{/*
Define nodeSelector property
*/}}
{{- define "eric-am-common-wfs.nodeSelector" -}}
  {{- include "eric-eo-evnfm-library-chart.nodeSelector" . -}}
{{- end -}}

{{/*
Create chart version as used by the chart label.
*/}}
{{- define "eric-am-common-wfs.version" -}}
{{- printf "%s" .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Kubernetes labels
*/}}
{{- define "eric-am-common-wfs.kubernetes-labels" -}}
app.kubernetes.io/name: {{ include "eric-am-common-wfs.name" . }}
app.kubernetes.io/instance: {{ .Release.Name | quote }}
app.kubernetes.io/version: {{ include "eric-am-common-wfs.version" . }}
{{- end -}}

{{/*
Common labels
*/}}
{{- define "eric-am-common-wfs.labels" -}}
  {{- $kubernetesLabels := include "eric-am-common-wfs.kubernetes-labels" . | fromYaml -}}
  {{- $globalLabels := (.Values.global).labels -}}
  {{- $serviceLabels := .Values.labels -}}
  {{- include "eric-eo-evnfm-library-chart.mergeLabels" (dict "location" .Template.Name "sources" (list $kubernetesLabels $globalLabels $serviceLabels)) }}
{{- end -}}

{{/*
Merged labels for extended defaults
*/}}
{{- define "eric-am-common-wfs.labels.extended-defaults" -}}
  {{- $extendedLabels := dict -}}
  {{- $_ := set $extendedLabels "logger-communication-type" "direct" -}}
  {{- $_ := set $extendedLabels "app" (include "eric-am-common-wfs.name" .) -}}
  {{- $_ := set $extendedLabels "chart" (include "eric-am-common-wfs.chart" .) -}}
  {{- $_ := set $extendedLabels "release" (.Release.Name) -}}
  {{- $_ := set $extendedLabels "heritage" (.Release.Service) -}}
  {{- $commonLabels := include "eric-am-common-wfs.labels" . | fromYaml -}}
  {{- $serviceMesh := include "eric-am-common-wfs.service-mesh-inject" . | fromYaml -}}
  {{- include "eric-eo-evnfm-library-chart.mergeLabels" (dict "location" .Template.Name "sources" (list $commonLabels $extendedLabels $serviceMesh)) | trim }}
{{- end -}}

{{/*
Create Ericsson product specific annotations
*/}}
{{- define "eric-am-common-wfs.helm-annotations_product_name" -}}
  {{- include "eric-eo-evnfm-library-chart.helm-annotations_product_name" . -}}
{{- end -}}

{{- define "eric-am-common-wfs.helm-annotations_product_number" -}}
  {{- include "eric-eo-evnfm-library-chart.helm-annotations_product_number" . -}}
{{- end -}}

{{- define "eric-am-common-wfs.helm-annotations_product_revision" -}}
  {{- include "eric-eo-evnfm-library-chart.helm-annotations_product_revision" . -}}
{{- end -}}

{{/*
Create a dict of annotations for the product information (DR-D1121-064, DR-D1121-067).
*/}}
{{- define "eric-am-common-wfs.product-info" -}}
ericsson.com/product-name: {{ template "eric-am-common-wfs.helm-annotations_product_name" . }}
ericsson.com/product-number: {{ template "eric-am-common-wfs.helm-annotations_product_number" . }}
ericsson.com/product-revision: {{ template "eric-am-common-wfs.helm-annotations_product_revision" . }}
{{- end -}}

{{/*
Common annotations
*/}}
{{- define "eric-am-common-wfs.annotations" -}}
  {{- $productInfo := include "eric-am-common-wfs.product-info" . | fromYaml -}}
  {{- $globalAnn := (.Values.global).annotations -}}
  {{- $serviceAnn := .Values.annotations -}}
  {{- include "eric-eo-evnfm-library-chart.mergeAnnotations" (dict "location" .Template.Name "sources" (list $productInfo $globalAnn $serviceAnn)) | trim }}
{{- end -}}

{{/*
Define probes
*/}}
{{- define "eric-am-common-wfs.probes" -}}
{{- $default := .Values.probes -}}
{{- if .Values.probing }}
  {{- if .Values.probing.liveness }}
    {{- if .Values.probing.liveness.commonwfs }}
      {{- $default := mergeOverwrite $default.commonwfs.livenessProbe .Values.probing.liveness.commonwfs -}}
    {{- end }}
  {{- end }}
  {{- if .Values.probing.readiness }}
    {{- if .Values.probing.readiness.commonwfs }}
      {{- $default := mergeOverwrite $default.commonwfs.readinessProbe .Values.probing.readiness.commonwfs -}}
    {{- end }}
  {{- end }}
{{- end }}
{{- $default | toJson -}}
{{- end -}}

{{/*
To support Dual stack.
*/}}
{{- define "eric-am-common-wfs.internalIPFamily" -}}
  {{- include "eric-eo-evnfm-library-chart.internalIPFamily" . -}}
{{- end -}}

{{/*
Define podPriority property
*/}}
{{- define "eric-am-common-wfs.podPriority" -}}
  {{- include "eric-eo-evnfm-library-chart.podPriority" ( dict "ctx" . "svcName" "commonwfs" ) -}}
{{- end -}}

{{/*
Define tolerations property
*/}}
{{- define "eric-am-common-wfs.tolerations.commonwfs" -}}
    {{- include "eric-eo-evnfm-library-chart.merge-tolerations" (dict "root" . "podbasename" "commonwfs" ) -}}
{{- end -}}

{{/*
Define DB connection pool max life time property
If not set by user, defaults to 14 minutes.
*/}}
{{ define "eric-am-common-wfs.db.connection.pool.max.lifetime" -}}
- name: "spring.datasource.hikari.max-lifetime"
  value: {{ index .Values "global" "db" "connection" "max-lifetime" | default "840000" | quote -}}
{{- end -}}

{{/*
Check global.security.tls.enabled
*/}}
{{- define "eric-am-common-wfs.global-security-tls-enabled" -}}
  {{- include "eric-eo-evnfm-library-chart.global-security-tls-enabled" . -}}
{{- end -}}

{{/*
DR-D470217-007-AD This helper defines whether this service enter the Service Mesh or not.
*/}}
{{- define "eric-am-common-wfs.service-mesh-enabled" }}
    {{- include "eric-eo-evnfm-library-chart.service-mesh-enabled" . -}}
{{- end -}}

{{/*
DR-D470217-011 This helper defines the annotation which bring the service into the mesh.
*/}}
{{- define "eric-am-common-wfs.service-mesh-inject" }}
  {{- include "eric-eo-evnfm-library-chart.service-mesh-inject" . -}}
{{- end -}}

{{/*
This helper defines log level for Service Mesh.
*/}}
{{- define "eric-am-common-wfs.service-mesh-logs" }}
  {{- include "eric-eo-evnfm-library-chart.service-mesh-logs" . -}}
{{- end -}}

{{/*
GL-D470217-080-AD
This helper captures the service mesh version from the integration chart to
annotate the workloads so they are redeployed in case of service mesh upgrade.
*/}}
{{- define "eric-am-common-wfs.service-mesh-version" }}
  {{- include "eric-eo-evnfm-library-chart.service-mesh-version" . -}}
{{- end -}}

{{/*
DR-D1123-124
Evaluating the Security Policy Cluster Role Name
*/}}
{{- define "eric-am-common-wfs.securityPolicy.reference" -}}
  {{- include "eric-eo-evnfm-library-chart.securityPolicy.reference" . -}}
{{- end -}}

{{/*
Define RedisCluster port
*/}}
{{ define "eric-am-common-wfs.redis.port" -}}
{{ $redisPort := .Values.redis.port -}}
{{- if .Values.global -}}
    {{- if .Values.global.security -}}
        {{- if .Values.global.security.tls -}}
            {{- if .Values.global.security.tls.enabled -}}
            {{- $redisPort = .Values.redis.tlsPort -}}
            {{- end -}}
        {{- end -}}
    {{- end -}}
{{- end -}}
{{- $redisPort -}}
{{- end -}}

{{/*
Name of the secret holding Redis ACL username and password
*/}}
{{- define "eric-am-common-wfs.redis.acl.secretname" }}
  {{- printf "%s-secret-%s" .Values.redis.host .Values.redis.acl.user -}}
{{- end }}

{{/*
Create fsGroup Values DR-1123-136
*/}}
{{- define "eric-am-common-wfs.fsGroup" -}}
  {{- include "eric-eo-evnfm-library-chart.fsGroup" . -}}
{{- end -}}

{{/*
DR-D470222-010
Configuration of Log Collection Streaming Method
*/}}
{{- define "eric-am-common-wfs.log.streamingMethod" -}}
  {{- include "eric-eo-evnfm-library-chart.log.streamingMethod" . -}}
{{- end }}

{{/*
Istio excludeOutboundPorts. Outbound ports to be excluded from redirection to Envoy.
*/}}
{{- define "eric-am-common-wfs.excludeOutboundPorts" -}}
  {{- include "eric-eo-evnfm-library-chart.excludeOutboundPorts" . -}}
{{- end -}}

{{/*
DR-D1123-134
Generation of role bindings for admission control in OpenShift environment
*/}}
{{- define "eric-am-common-wfs.service-account.name" -}}
  {{- printf "%s-sa" (include "eric-am-common-wfs.name" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
DR-D1123-134
Rolekind parameter for generation of role bindings for admission control in OpenShift environment
*/}}
{{- define "eric-am-common-wfs.securityPolicy.rolekind" }}
  {{- include "eric-eo-evnfm-library-chart.securityPolicy.rolekind" . }}
{{- end }}

{{/*
DR-D1123-134
Rolename parameter for generation of role bindings for admission control in OpenShift environment
*/}}
{{- define "eric-am-common-wfs.securityPolicy.rolename" }}
  {{- include "eric-eo-evnfm-library-chart.securityPolicy.rolename" . }}
{{- end }}

{{/*
DR-D1123-134
RoleBinding name for generation of role bindings for admission control in OpenShift environment
*/}}
{{- define "eric-am-common-wfs.securityPolicy.rolebinding.name" }}
  {{- include "eric-eo-evnfm-library-chart.securityPolicy.rolebinding.name" . }}
{{- end }}

{{- define "eric-am-common-wfs.camunda.bpm.historyLevel" -}}
  {{- $defaultLevel := "activity" -}}
  {{- $requestedLevel := .Values.camunda.historyLevel -}}
  {{- if or (eq $requestedLevel "audit") (eq $requestedLevel "full") (eq $requestedLevel "none") -}}
    {{- $requestedLevel -}}
  {{- else -}}
    {{- $defaultLevel -}}
  {{- end -}}
{{- end -}}
