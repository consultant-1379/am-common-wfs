#!/bin/bash
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



mkdir individualCerts && cd $_
cp /run/secrets/ssl/certs/ca.crt  /etc/ssl/certs/ca.crt
cat /etc/ssl/certs/ca.crt >> ca.crt
FILE_COUNT=$(csplit -f individual- ca.crt '/-----BEGIN CERTIFICATE-----/' '{*}' --elide-empty-files | wc -l)
echo "Number of certs in cacert bundle is ${FILE_COUNT}"
for i in $(ls); do
  echo "Adding ${i} to java keystore"
  keytool -storepass 'changeit' -noprompt -trustcacerts -importcert -file ${i} -alias ${i} -keystore /var/lib/ca-certificates/java-cacerts 2>&1
done
cd ../
rm -rf individualCerts

exec java -Dspring.profiles.active=prod -Djdk.tls.client.protocols="TLSv1.3,TLSv1.2" -Djava.security.egd=file:/dev/./urandom -jar /eric-am-common-wfs.jar &

wait
