#
# COPYRIGHT Ericsson 2024
#
#
#
# The copyright to the computer program(s) herein is the property of
#
# Ericsson Inc. The programs may be used and/or copied only with written
#
# permission from Ericsson Inc. or in accordance with the terms and
#
# conditions stipulated in the agreement/contract under which the
#
# program(s) have been supplied.
#

ARG BASE_IMAGE_VERSION
FROM armdocker.rnd.ericsson.se/proj-am/sles/sles-corretto-openjdk17:${BASE_IMAGE_VERSION}

ARG GIT_COMMIT=""
ARG APP_VERSION=""
ARG BUILD_TIME=""
ARG HELM_VERSIONS="3.8.1 3.10.1 3.12.0 3.13.0 3.14.2"
ARG HELM_DEFAULT_VERSION="3.8"
ARG HELM_LATEST_VERSION="3.14"
ARG HELM_REPO="https://arm.sero.gic.ericsson.se/artifactory"
ARG WFS_DATA_DIR="/wfs"
# User/Group Id generated based on ADP rule DR-D1123-122 (eric-am-common-wfs : 291171)
ARG WFS_GID=291171
ARG WFS_UID=291171

LABEL product.number="CXU 101 0680" \
      product.revision="R1A" \
      GIT_COMMIT=$GIT_COMMIT \
      com.ericsson.product-name="EVNFM WFS Service" \
      com.ericsson.product-number="CXU 101 0680" \
      com.ericsson.product-revision="R1A" \
      org.opencontainers.image.title="EVNFM WFS Service" \
      org.opencontainers.image.created=${BUILD_TIME} \
      org.opencontainers.image.revision=${GIT_COMMIT} \
      org.opencontainers.image.version=${APP_VERSION} \
      org.opencontainers.image.vendor="Ericsson"

RUN zypper install -l -y shadow util-linux && \
    echo "${WFS_UID}:x:${WFS_UID}:${WFS_GID}:wfs-user:/:/bin/false" >> /etc/passwd && \
    sed -i '/root/s/bash/false/g' /etc/passwd

RUN for VERSION in ${HELM_VERSIONS}; \
    do \
      VERSION_ARCHIVE=helm-${VERSION}.tar.gz; \
      VERSION_DIR=helm-${VERSION}; \
      VERSION_SUFFIX=${VERSION%.*}; \
      VERSION_NAME=helm-${VERSION_SUFFIX}; \
      curl -SsL ${HELM_REPO}/get-helm/helm-v${VERSION}-linux-amd64.tar.gz -o ${VERSION_ARCHIVE}; \
      mkdir -p ${VERSION_DIR}; tar -zxf ${VERSION_ARCHIVE} -C ${VERSION_DIR}; \
      cp ${VERSION_DIR}/linux-amd64/helm /usr/local/bin/${VERSION_NAME}; \
      rm -f ${VERSION_ARCHIVE}; \
      rm -rf ${VERSION_DIR}; \
    done && \
    ln -s helm-${HELM_DEFAULT_VERSION} /usr/local/bin/helm; \
    ln -s helm-${HELM_LATEST_VERSION} /usr/local/bin/helm-latest; \
    chmod -R g=u /usr/local/bin/helm* && \
    chown -h $WFS_UID:0 /usr/local/bin/helm*;

COPY entryPoint.sh /entryPoint.sh
ADD eric-am-common-wfs-server/target/eric-am-common-wfs.jar eric-am-common-wfs.jar

RUN touch /eric-am-common-wfs.jar && touch /camunda-command-executor.jar && \
    chmod 755 /entryPoint.sh /var/lib/ca-certificates/java-cacerts && \
    mkdir -p "$WFS_DATA_DIR" && \
    chmod -R g=u "$WFS_DATA_DIR" && \
    chown -fR $WFS_UID:0 "$WFS_DATA_DIR" && \
    chmod -R g=u /var/lib/ca-certificates/java-cacerts && \
    chown -fR $WFS_UID:0 /var/lib/ca-certificates/java-cacerts

USER $WFS_UID:$WFS_GID
WORKDIR $WFS_DATA_DIR

ENTRYPOINT ["/bin/sh"]
CMD ["/entryPoint.sh"]

EXPOSE 8888
