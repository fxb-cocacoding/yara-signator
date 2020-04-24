# YARA-Signator
Automatic YARA rule generation for Malpedia

## Disclaimer
The software is running and compiles, but isn't heavily tested. There is not written a single test yet. You probably should not use it on production systems. But it should provide interesting results at this point in time. If the following guide is not working, do not hesitate to contact me (here, mail, twitter, linked, etc). The next week and months, the software will improve (and the documentation, too).

A full run over all malpedia samples on a SATA-HDD without RAID and 20GB memory running yara-signator, capstone_server and postgres on the same device takes around 10h. The insertion takes around 3-4h, the database operations 4-6h and the YARA rule derivation around 2-3h.

## Hardware requirements:
If you run postgres and yara-signator on one machine, I would recommend you to have at least 16GB memory and your system is nearly headless. Postgres will be very slow with less than 8GB memory and yara-signator uses many threads and will consume up to 6GB memory very quick. If you set the threads to 1 you will have a very low memory usage but low performance.

The full chain will create database files up to 100GB very quickly, so you should have at least 100GB space left. The speed should be dramatically increased if your space is on faster storage.


## Getting started:

**NEW:** See our new wiki and the installation guide:

https://github.com/fxb-cocacoding/yara-signator/wiki/YARA-Signator-%23-User-Manual-for-Version-0.3.1


## Workflow

IMPORTANT: Make sure you have not a database in postgres using the same name as mentioned in the config file. The first thing yara-signator will do is a database drop if you have not activated the `skipSMDAInsertions` using a `true` flag.

1. Get SMDA reports from your malware pool. (https://github.com/danielplohmann/smda)
2. Create a valid config file (set folders to smda reports and to your pool, etc).
3. Start postgresql daemon
4. Start capstone_server on port 12345
5. Launch yara-signator (`java -jar target/yara-signator-0.3.1-SNAPSHOT-jar-with-dependencies.jar >> logfile.txt`)
6. Monitor your log file: `tail -F logfile.txt`
7. Check if capstone_server crashed, if yes, restart it


## FAQ
Q: This capstone_server is a C program, why don't you use the capstone bindings for JAVA like everyone else?<br />
A: I had some strange memory leaks (>10GB lost memory) when running many millions of opcodes against capstone over some hours. I couldn't fix them in JAVA and so I created a small program in C which communicates over TCP with other processes. If this process would have any memory leaks, you could simply restart it and the memory would be released back to the OS.


## Report
The approach is described in detail in my BS thesis:
http://cocacoding.com/papers/Automatic_Generation_of_code_based_YARA_Signatures.pdf <br />
SHA512: <br /> 0384d6ec497cbfca2ec4a7739337088c8c859e86ca63339fd3d26c8be2176e3378210ac6cbd725d08a2f672e9cb2dcecc09eef2ec2dbc975de726b0b918795b2 <br />
SHA256: <br />
4f0530d0da48b394cb0798c434ffc70c33ae351e54c77454c87546d17ec52b60 <br />


This project was mainly developed during the writing of the previously mentioned BS thesis. According to this I would like to thank Daniel Plohmann for his great support and helpful ideas during his supervision and beyond, especially regarding to this software project.

