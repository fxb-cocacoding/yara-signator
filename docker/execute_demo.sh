#!/bin/bash

cd datastore
./initialize.sh
cd ..

sudo docker-compose -f test-compose.yaml up --build --force-recreate --no-deps
