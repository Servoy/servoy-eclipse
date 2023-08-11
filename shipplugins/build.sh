#!/bin/bash

set -ex

MVNARGS=-DskipTests=true

export CHROME_BIN=/snap/bin/chromium

export JAVA_HOME=/data/programs/java/jdk-11.0.3


#mvn install:install-file -Dfile=com.ibm.icu-3.8.1.v20080530.jar \
#                        -DgroupId=com.ibm \
#                        -DartifactId=com.ibm.icu \
#                        -Dversion=3.8.1 \
#                       -Dpackaging=jar

#cd tmp
#jar cmf META-INF/MANIFEST.MF ../hibernate-patched.jar org
#cd ..

#mvn install:install-file -Dfile=hibernate-patched.jar \
#                        -DgroupId=org.hibernate \
#                        -DartifactId=hibernate-core \
#                        -Dversion=5.6.15.Servoy2 \
#                       -Dpackaging=jar



mvn clean install
