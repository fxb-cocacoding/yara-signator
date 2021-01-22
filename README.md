# YARA-Signator

Automatic YARA rule generation for malware repositories. Currently used to build YARA signatures for Malpedia (https://malpedia.caad.fkie.fraunhofer.de) and limited to x86/x86-64 executables and memory dumps for Linux, macOS and Windows.


## Target Audience

This software is useful for larger organizations like companies or CERTs as well as for indivuduals. It only requires a modern, personal computer (8 cores/threads and 16 GiB recommended) and a curated malware repository. Curated means in this context that all samples are already sorted and clustered to families. Each family can contain various samples. In general the tool works better for unpacked malware because we try to detect special code regions or functions that identify a given family.


## Preconditions

You have a curated malware repository, formatted like this:

```
./malware-repo/<malware_family_name>/<sample_name_or_hash>
```

You build disassembly reports with smda (https://github.com/danielplohmann/smda) and store them as input files for YARA-Signator. We will need both, the malware repo in the respective folder structure and the reports. The reports will be consumed to evaluate the binary code of the samples to generate possible sequence candidates that determine the family. Those are checked against the rpository to increase the quality significantly.

When the preconditions are met you may start the process and run YARA-Signator as stated in chapter -> Workflow.


## Hardware requirements

If you run postgres and yara-signator on one machine, I would recommend you to have at least 16GB memory and your system is nearly headless. Postgres will be very slow with less than 8GB memory and yara-signator uses many threads and will consume up to 6GB memory very quick. If you set the threads to 1 you will have a very low memory usage but this will result in low performance.

The full chain will create database files up to 100GB very quickly, so you should have at least 100GB space left. The speed should be dramatically increased if your space is on faster storage.

A full run over all malpedia samples on a SATA-HDD without RAID and 20GB memory running yara-signator, capstone_server and postgres on the same device takes around 10h. The insertion takes around 3-4h, the database operations 4-6h and the YARA rule derivation around 2-3h.


## Getting started

See our new wiki and the installation guide:
https://github.com/fxb-cocacoding/yara-signator/wiki/YARA-Signator-%23-User-Manual-for-Version-0.6.0


## Workflow

IMPORTANT: Make sure you have not a database in postgres using the same name as mentioned in the config file. The first thing yara-signator will do is a database drop if you have not activated the `skipSMDAInsertions` using a `true` flag.

1. Get SMDA reports from your malware pool. (https://github.com/danielplohmann/smda)
2. Create a valid config file (set folders to smda reports and to your pool, etc).
3. Start postgresql daemon
4. Start capstone_server on port 12345, using the daemonize script
5. Launch yara-signator (`java -jar target/yara-signator-0.5.0-SNAPSHOT-jar-with-dependencies.jar >> logfile.txt`)
6. Monitor your log file: `tail -F logfile.txt`


## FAQ

Q: This capstone_server is a C program, why don't you use the capstone bindings for JAVA like everyone else?<br />
A: I had some strange memory leaks (>10GB lost memory) when running many millions of opcodes against capstone over some hours. I couldn't fix them in JAVA and so I created a small program in C which communicates over TCP with other processes. If this process would have any memory leaks, you could simply restart it and the memory would be released back to the OS.


## Presentation at Botconf 2019:

See the slides of our presentation for a short introduction of YARA-Signator and how it works:
https://www.botconf.eu/wp-content/uploads/2019/12/B2019-Bilstein-Plohmann-YaraSignator.pdf


## Paper

See our paper for more information:
https://journal.cecyf.fr/ojs/index.php/cybin/article/view/24


## BS-Thesis

The approach is described in detail in my BS thesis:
http://cocacoding.com/papers/Automatic_Generation_of_code_based_YARA_Signatures.pdf <br />
SHA512: 0384d6ec497cbfca2ec4a7739337088c8c859e86ca63339fd3d26c8be2176e3378210ac6cbd725d08a2f672e9cb2dcecc09eef2ec2dbc975de726b0b918795b2 <br />
SHA256: 4f0530d0da48b394cb0798c434ffc70c33ae351e54c77454c87546d17ec52b60 <br />

This project was mainly developed during the writing of the previously mentioned BS thesis. According to this I would like to thank Daniel Plohmann for his great support and helpful ideas during his supervision and beyond, especially regarding to this software project.

