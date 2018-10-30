#!/bin/bash -e

echo "##teamcity[progressMessage 'sbt $@']"

export JDK_HOME=/usr/lib/jvm/java-1.8.0-openjdk-amd64
export JAVA_HOME=${JDK_HOME}

echo "********** Java version **********"
${JAVA_HOME}/bin/java -version
echo "**********************************"

${JAVA_HOME}/bin/java \
    -Dfile.encoding=UTF8 \
    -Xmx1G \
    -XX:+UseCompressedOops \
    -Dsbt.log.noformat=true \
    -Djava.awt.headless=true \
    -jar sbt-launch.jar "$@"