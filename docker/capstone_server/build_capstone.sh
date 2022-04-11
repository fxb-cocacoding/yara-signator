#!/bin/sh
git clone https://github.com/fxb-cocacoding/capstone_server.git
cd capstone_server
mkdir build
cd build
cmake ..
make all
ls -lah
