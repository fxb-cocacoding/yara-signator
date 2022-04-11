#!/bin/bash
if [[ $# -ne 2 ]]; then
  echo $@
  echo "Error. Wrong usage!"
  exit 1
fi
echo "==== CAPSTONE SERVER ===="
echo $1
echo $2
echo "-------------------------"
cd capstone_server
ip -c a
echo "IP: "$1" port: "$2" "
./daemonize.sh "$1" "$2"
process_id=$!
echo "Launched at "$process_id""
sleep 1
echo "Waiting for "$process_id" now..."
wait $process_id
