FROM ubuntu:latest

ENV DEBIAN_FRONTEND=noninteractive
RUN DEBIAN_FRONTEND="noninteractive" apt-get update && apt-get -y install flex cmake bash bison byacc git make netcat-openbsd python3 python3-dev iproute2 psmisc net-tools openjdk-11-jdk maven iputils-ping build-essential dh-autoreconf libssl-dev python3-venv

WORKDIR /root
COPY build_all.sh build_all.sh
COPY configuration.json '.yarasignator.conf'
RUN ./build_all.sh

RUN cat .yarasignator.conf
CMD [ "java", "-Xms2G", "-Xmx8G", "-jar", "yara-signator/target/yara-signator-0.6.0-SNAPSHOT-jar-with-dependencies.jar" ]
