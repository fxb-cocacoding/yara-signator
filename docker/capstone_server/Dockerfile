FROM ubuntu:latest
WORKDIR /code
ENV DEBIAN_FRONTEND=noninteractive 
RUN DEBIAN_FRONTEND="noninteractive" TZ="Europe/Brussels" apt-get update && apt-get -y install tzdata gcc g++ libcapstone-dev cmake bash git make netcat-openbsd python3 iproute2 psmisc net-tools iputils-ping
EXPOSE 12345

#
# Change default user, we don't need root privileges
#

RUN groupadd --gid 5001 capstone
RUN useradd --home-dir /home/capstone \
      --create-home --uid 5001 --gid 5001 \
      --shell /bin/sh --skel /dev/null capstone
WORKDIR /home/capstone
USER capstone

COPY --chown=capstone:capstone build_capstone.sh build_capstone.sh
COPY --chown=capstone:capstone run_capstone_server.sh run_capstone_server.sh
RUN ./build_capstone.sh
#COPY --chown=capstone:capstone . .
RUN ls -lah
RUN ifconfig
CMD [ "./run_capstone_server.sh", "0.0.0.0", "12345" ]
