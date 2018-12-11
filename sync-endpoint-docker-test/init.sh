#!/bin/bash -eu

set -o pipefail

if [ -f /org.opendatakit.sync.ldapcert ]; then
    echo "Adding LDAP CA Certificate"
    keytool -noprompt -import -trustcacerts -alias ldapcert -keystore "/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/security/cacerts" -file /org.opendatakit.sync.ldapcert -storepass changeit
fi

exec "$@"
