{{/*
Helm executor job template
*/}}
{{- define "eric-am-common-wfs.helm-executor-job" -}}
apiVersion: batch/v1
kind: Job
metadata:
  name: {{ template "eric-am-common-wfs.name" . }}-helm-executor-job
  labels: {{- include "eric-am-common-wfs.helm-executor.labels.extended-defaults" . | nindent 4 }}
  annotations: {{- include "eric-am-common-wfs.helm-executor.annotations" . | nindent 4 }}
spec:
  ttlSecondsAfterFinished: {{ .Values.helmExecutor.job.ttlSecondsAfterFinished }}
  backoffLimit: 0
  template:
    metadata:
      labels: {{- include "eric-am-common-wfs.helm-executor.labels.extended-defaults" . | nindent 8 }}
      annotations: {{- include "eric-am-common-wfs.helm-executor.annotations" . | nindent 8 }}
    spec:
      restartPolicy: Never
      {{- if include "eric-am-common-wfs.pullSecrets" . }}
      imagePullSecrets:
        - name: {{ template "eric-am-common-wfs.pullSecrets" . }}
      {{- end }}
      serviceAccountName: {{ template "eric-am-common-wfs.name" . }}-helm-executor-sa
      automountServiceAccountToken: false
      priorityClassName: {{- include "eric-am-common-wfs.helm-executor.podPriority" . | indent 2 }}
      containers:
        - name: {{ template "eric-am-common-wfs.helm-executor.name" . }}
          image: {{ template "eric-am-common-wfs.helm-executor.imagePath" . }}
          imagePullPolicy: {{ template "eric-am-common-wfs.helm-executor.imagePullPolicy" . }}
          command: ["/bin/bash", "-c"]
          args:
           - |
             cp /run/secrets/ssl/certs/ca.crt  /etc/ssl/certs/ca.crt
             /usr/bin/helm-executor
          env:
            - name: "XDG_CACHE_HOME"
              value: "/helm-executor"
            - name: "REDIS_USERNAME"
              valueFrom:
                secretKeyRef:
                  name: {{ include "eric-am-common-wfs.redis.acl.secretname" . | quote }}
                  key: {{ .Values.redis.acl.userKey | quote }}
            - name: "REDIS_PASSWORD"
              valueFrom:
                secretKeyRef:
                  name: {{ include "eric-am-common-wfs.redis.acl.secretname" . | quote }}
                  key: {{ .Values.redis.acl.passKey | quote }}
            - name: "REDIS_HOST"
              value: {{ .Values.redis.host }}
            - name: "REDIS_PORT"
              value: {{ include "eric-am-common-wfs.redis.port" . | quote }}
            - name: "CRYPTO_HOST"
              value: "http://eric-eo-evnfm-crypto"
            - name: WFS_CAMUNDA_URL
              value: "{{ .Values.helmExecutor.wfs.camunda.url }}"
            - name: LOGSTASH_HOST
              value: "{{ .Values.logging.logstash.host }}"
            - name: LOGSTASH_PORT
              value: "{{ .Values.logging.logstash.syslogPort }}"
            - name: HELM_DEBUG
              value: {{ .Values.helm.debug.enabled | quote }}
          securityContext:
            readOnlyRootFilesystem: true
            allowPrivilegeEscalation: false
            runAsNonRoot: true
            runAsGroup: 155463
            capabilities:
              drop:
                - all
          volumeMounts:
            - name: create-cacert-volume
              mountPath: /run/secrets/ssl/certs/
            - name: executor-cacert
              mountPath: /etc/ssl/certs/
            - name: tmp-data
              mountPath: /tmp
            - name: helm-executor
              mountPath: /helm-executor
          resources:
            requests:
            {{- if .Values.resources.helmExecutor.requests.cpu }}
              cpu: {{ .Values.resources.helmExecutor.requests.cpu | quote }}
            {{- end }}
            {{- if .Values.resources.helmExecutor.requests.memory }}
              memory: {{ .Values.resources.helmExecutor.requests.memory | quote }}
            {{- end }}
            {{- if index .Values.resources.helmExecutor.requests "ephemeral-storage" }}
              ephemeral-storage: {{ index .Values.resources.helmExecutor.requests "ephemeral-storage" | quote }}
            {{- end }}
            limits:
            {{- if .Values.resources.helmExecutor.limits.cpu }}
              cpu: {{ .Values.resources.helmExecutor.limits.cpu | quote }}
            {{- end }}
            {{- if .Values.resources.helmExecutor.limits.memory }}
              memory: {{ .Values.resources.helmExecutor.limits.memory | quote }}
            {{- end }}
            {{- if index .Values.resources.helmExecutor.limits "ephemeral-storage" }}
              ephemeral-storage: {{ index .Values.resources.helmExecutor.limits "ephemeral-storage" | quote }}
            {{- end }}
      volumes:
        - name: helm-executor
          emptyDir: {}
        - name: executor-cacert
          emptyDir: {}
        - name: tmp-data
          emptyDir: {}
        - name: create-cacert-volume
          secret:
            secretName: {{ .Values.iam.cacert.secretName }}
            items:
              - key: {{ .Values.iam.cacert.key }}
                path: {{ .Values.iam.cacert.filePath }}
      {{- if .Values.nodeSelector }}
      nodeSelector: {{- include "eric-am-common-wfs.nodeSelector" . | nindent 8 }}
      {{- else if .Values.global.nodeSelector }}
      nodeSelector: {{- include "eric-am-common-wfs.nodeSelector" . | nindent 8 }}
      {{- end }}
      {{- if or .Values.tolerations (and .Values.global .Values.global.tolerations) }}
      tolerations: {{ include "eric-am-common-wfs.tolerations.helmExecutor" . | nindent 8 }}
      {{- end }}
{{- end }}