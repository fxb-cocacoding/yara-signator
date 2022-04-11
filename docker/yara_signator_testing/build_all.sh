#!/bin/sh

#
# Install java2yara:
#
cd /home/signator
home=$(pwd)

rm -rf java2yara
git clone https://github.com/fxb-cocacoding/java2yara.git
cd java2yara
mvn package
mvn install:install-file -Dfile=target/java2yara-0.6.0-SNAPSHOT.jar -DpomFile=pom.xml
cd ..

#
# Install smda-reader:
#

rm -rf smda-reader
git clone https://github.com/fxb-cocacoding/smda-reader.git
cd smda-reader
mvn package
mvn install:install-file -Dfile=target/smda-reader-0.6.0-SNAPSHOT.jar -DpomFile=pom.xml
cd ..

#
# Build YARA-Signator:
#

rm -rf yara-signator
git clone https://github.com/fxb-cocacoding/yara-signator.git
cd yara-signator
mvn -DskipTests package
cd ..

cd $home
ls -lah
mkdir -p datastore
cd datastore
touch test

cd $home
mkdir -p git
cd git
rm -rf yara
git clone https://github.com/VirusTotal/yara.git
cd yara
git fetch --all --tags
git checkout tags/v3.8.0
# Build yara
./build.sh

cd $home
ls yara-signator/target
ls -lah */*
