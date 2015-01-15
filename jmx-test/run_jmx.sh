#!/usr/bin/env bash

#
# Simple test script for testing remote JMX
#
# -Dcom.sun.management.jmxremote 
# -Dcom.sun.management.jmxremote.port=9999 
# -Dcom.sun.management.jmxremote.rmi.port=9998 
# -Dcom.sun.management.jmxremote.ssl=false 
# -Dcom.sun.management.jmxremote.authenticate=false 
# -Djava.rmi.server.hostname=<public EC2 hostname>

#SPARK_PUBLIC_DNS="192.168.1.2"
export SPARK_PUBLIC_DNS="`wget -q -O - http://169.254.169.254/latest/meta-data/public-ipv4`"

echo "My public IP is: ${SPARK_PUBLIC_DNS}"

jmx_opt="-Dcom.sun.management.jmxremote"
jmx_opt="${jmx_opt} -Djava.net.preferIPv4Stack=true"
jmx_opt="${jmx_opt} -Dcom.sun.management.jmxremote.port=9999"
jmx_opt="${jmx_opt} -Dcom.sun.management.jmxremote.rmi.port=9998"
jmx_opt="${jmx_opt} -Dcom.sun.management.jmxremote.ssl=false"
jmx_opt="${jmx_opt} -Dcom.sun.management.jmxremote.authenticate=false"
jmx_opt="${jmx_opt} -Djava.rmi.server.hostname=${SPARK_PUBLIC_DNS}"

export SPARK_DAEMON_JAVA_OPTS="${jmx_opt}"
echo "${SPARK_DAEMON_JAVA_OPTS}"

# now run the smaple program
javac JmxTest.java
java ${SPARK_DAEMON_JAVA_OPTS} JmxTest
