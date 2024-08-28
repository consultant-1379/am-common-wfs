{{/*
Define helm executor name.
*/}}
{{- define "eric-am-common-wfs.helm-executor.name" -}}
  {{- $productInfo := fromYaml (.Files.Get "eric-product-info.yaml") -}}
  {{- $name := $productInfo.images.helmExecutor.name -}}
  {{- printf "%s" $name -}}
{{- end -}}


{{/*
Define pullPolicy for helm executor container
*/}}
{{- define "eric-am-common-wfs.helm-executor.imagePullPolicy" -}}
   {{- include "eric-eo-evnfm-library-chart.imagePullPolicy" (dict "ctx" . "svcRegistryName" "helmExecutor") -}}
{{- end -}}


{{/*
Define helm executor image registry url.
*/}}
{{- define "eric-am-common-wfs.helm-executor.imagePath" -}}
    {{- include "eric-eo-evnfm-library-chart.mainImagePath" (dict "ctx" . "svcRegistryName" "helmExecutor") -}}
{{- end -}}


{{/*
Common annotations
*/}}
{{- define "eric-am-common-wfs.helm-executor.annotations" -}}
  {{- $productInfo := include "eric-am-common-wfs.product-info" . | fromYaml -}}
  {{- $globalAnn := (.Values.global).annotations -}}
  {{- $serviceAnn := .Values.annotations -}}
  {{- include "eric-eo-evnfm-library-chart.mergeAnnotations" (dict "location" .Template.Name "sources" (list $productInfo $globalAnn $serviceAnn)) | trim }}
{{- end -}}


{{/*
Kubernetes labels
*/}}
{{- define "eric-am-common-wfs.helm-executor.kubernetes-labels" -}}
app.kubernetes.io/name: {{ include "eric-am-common-wfs.helm-executor.name" . }}
app.kubernetes.io/instance: {{ .Release.Name | quote }}
app.kubernetes.io/version: {{ (fromYaml (.Files.Get "eric-product-info.yaml")).images.helmExecutor.tag | quote }}
{{- end -}}


{{/*
Common labels
*/}}
{{- define "eric-am-common-wfs.helm-executor.labels" -}}
  {{- $kubernetesLabels := include "eric-am-common-wfs.helm-executor.kubernetes-labels" . | fromYaml -}}
  {{- $globalLabels := (.Values.global).labels -}}
  {{- $serviceLabels := .Values.labels -}}
  {{- include "eric-eo-evnfm-library-chart.mergeLabels" (dict "location" .Template.Name "sources" (list $kubernetesLabels $globalLabels $serviceLabels)) }}
{{- end -}}


{{/*
Merged labels for extended defaults
*/}}
{{- define "eric-am-common-wfs.helm-executor.labels.extended-defaults" -}}
  {{- $extendedLabels := dict -}}
  {{- $_ := set $extendedLabels "app" (include "eric-am-common-wfs.helm-executor.name" .) -}}
  {{- $_ := set $extendedLabels "logger-communication-type" "direct" }}
  {{- $_ := set $extendedLabels "eric-cloud-native-kvdb-rd-operand-access" "true" }}
  {{- $_ := set $extendedLabels "eric-evnfm-job-type" "helm" }}
  {{- $commonLabels := include "eric-am-common-wfs.helm-executor.labels" . | fromYaml -}}
  {{- $serviceMesh := include "eric-am-common-wfs.service-mesh-inject-job" . | fromYaml }}
  {{- include "eric-eo-evnfm-library-chart.mergeLabels" (dict "location" .Template.Name "sources" (list $commonLabels $extendedLabels $serviceMesh)) | trim }}
{{- end -}}


{{- define "eric-am-common-wfs.helm-executor.podPriority" -}}
{{- if .Values.podPriority -}}
  {{- if .Values.podPriority.helmExecutor -}}
    {{- .Values.podPriority.helmExecutor.priorityClassName | toString -}}
  {{- end -}}
{{- end -}}
{{- end -}}


{{/*
Define tolerations property
*/}}
{{- define "eric-am-common-wfs.tolerations.helmExecutor" -}}
  {{- include "eric-eo-evnfm-library-chart.merge-tolerations" (dict "root" . "podbasename" "helmExecutor" ) -}}
{{- end -}}


{{/*
 DR-D470217-011 This helper defines the annotation to jobs with disabled istio proxy.
*/}}
{{- define "eric-am-common-wfs.service-mesh-inject-job" }}
sidecar.istio.io/inject: "false"
{{- end -}}