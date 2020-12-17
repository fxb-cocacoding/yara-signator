#!/bin/sh
cd ../smda-reader
mvn clean; mvn compile; mvn package
mvn install:install-file -Dfile=target/smda-reader-0.6.0-SNAPSHOT.jar -DpomFile=pom.xml

cd ../java2yara
mvn clean; mvn compile; mvn package
mvn install:install-file -Dfile=target/java2yara-0.6.0-SNAPSHOT.jar -DpomFile=pom.xml

cd ../yara-signator
mvn clean; mvn compile; mvn package
mvn install:install-file -Dfile=target/yara-signator-0.6.0-SNAPSHOT.jar -DpomFile=pom.xml
