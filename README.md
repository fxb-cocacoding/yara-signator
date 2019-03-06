# yara-signator
Automatic YARA rule generation for Malpedia

## Disclaimer
The software is running and compiles, but isn't heavily tested. There is not written a single test yet. You probably should not use it on production systems. But it should provide interesting results at this point in time. If the following guide is not working, do not hesitate to contact me (here, mail, twitter, linked, etc). The next week and months, the software will improve (and the documentation, too).

A full run over all malpedia samples on a SATA-HDD without RAID and 20GB memory running yara-signator, capstone_server and postgres on the same device takes around 10h. The insertion takes around 3-4h, the database operations 4-6h and the YARA rule derivation around 2-3h.

## Hardware requirements:
If you run postgres and yara-signator on one machine, I would recommend you to have at least 16GB memory and your system is nearly headless. Postgres will be very slow with less than 8GB memory and yara-signator uses many threads and will consume up to 6GB memory very quick. If you set the threads to 1 you will have a very low memory usage but low performance.

The full chain will create database files up to 100GB very quickly, so you should have at least 100GB space left. The speed should be dramatically increased if your space is on faster storage.

## Getting started:
If you want to build yara-signator, you need java 8 (or higher), maven, smda-reader and java2yara.

https://openjdk.java.net/<br />
https://maven.apache.org/<br />
https://github.com/fxb-cocacoding/smda-reader<br />
https://github.com/fxb-cocacoding/java2yara<br />

To compile yara-signator, you need only to copy the libraries smda-reader and java2yara into your local maven store (if you don't want to compile it yourself, there are binary files provided in the target-folder):

```
git clone https://github.com/fxb-cocacoding/smda-reader.git
cd smda-reader
mvn package
mvn install:install-file -Dfile=target/smda-reader-0.0.1-SNAPSHOT.jar -DpomFile=pom.xml
```
and
```
git clone https://github.com/fxb-cocacoding/java2yara.git
cd java2yara
mvn package
mvn install:install-file -Dfile=target/java2yara-0.0.1-SNAPSHOT.jar -DpomFile=pom.xml
```

Then you can compile yara-signator:

```
git clone https://github.com/fxb-cocacoding/yara-signator.git
cd yara-signator
mvn package
```

Now you should have two (overwritten) jar-files in your target folder.

## Runtime dependencies
For using yara-signator, you need a postgres database (I tested postgres-10 only) and capstone_server.<br />
https://github.com/fxb-cocacoding/capstone_server<br />
https://www.postgresql.org/<br />

If you launch postgresql with default configuration (be sure that it runs on 127.0.0.1), you should have the user postgres and no password (currently one line is outcommented, it has password support in general). Yara-signator is able to populate the databases and tables itself, so there is nothing to change. In general, the postgres connection information and credentials are provided via the config file at `~/.yarasignator.conf `.

Then you have to launch capstone_server, as it is currently the only supported disassembler backend. If you haven't build it yet, you can do it this way:
```
git clone https://github.com/fxb-cocacoding/capstone_server.git
cd capstone_server
mkdir build
cd build
cmake ..
make all
```

## Config file
To use yara-signator, the tool will search for a configuration file located in your home folder called `.yarasignator.conf`. A sample file looks like this:

```
{
  "smda_path": "/home/user/yara-signator-testing/smda_reports/smda-malpedia-large-2019-02-04/",
  "malpedia_path": "/home/user/yara-signator-testing/malpedia/",
  "output_path": "/home/user/yara-signator-testing/yara_output/",
  "yaraBinary": "/usr/bin/yara",
  "yaracBinary": "/usr/bin/yarac",

  "db_connection_string": "jdbc:postgresql://127.0.0.1/",
  "db_user": "postgres",
  "db_password": "",
  "db_name": "caching_db",

  "skipSMDAInsertions": false,
  "skipUniqueNgramTableCreation": false,
  "skipYaraRuleGeneration": false,
  "skipRuleValidation": false,

  "insertion_threads": 16,
  "rulebuilder_threads": 8,

  "shuffle_seed": 12345678,
  "minInstructions": 10,
  "batchSize": 5000,
  "instructionLimitPerFamily": 15000000,

  "duplicatesInsideSamplesEnabled": false,
  "wildcardConfigEnabled": true,
  "rankingOptimizerEnabled": true,
  "scoreCommentEnabled": true,
  "prettifyEnabled": true,

  "wildcardConfig": [
    {
      "wildcardOperator": "onlyfirstbyte"
    },
    {
      "wildcardOperator": "dummy"
    }
  ],

  "rankingConfig": [
    {
      "ranker": "rankPerNgramScore",
      "limit": 1000
    },
    {
      "ranker": "dummyRanking",
      "limit": 10
    }
  ],

  "n": [
    4,
    5,
    6,
    7
  ]

}
```

IMPORTANT: Make sure you have not a database in postgres using the same name as mentioned in the config file. The first thing yara-signator will do is a database drop if you have not activated the `skipSMDAInsertions` using a `true` flag.

```
  "smda_path": "/home/user/yara-signator-testing/smda_reports/smda-malpedia/",
  "malpedia_path": "/home/user/yara-signator-testing/malpedia/",
  "output_path": "/home/user/yara-signator-testing/yara_output/",
  "yaraBinary": "/usr/bin/yara",
  "yaracBinary": "/usr/bin/yarac",
```
`smda_path` has to point to a folder containing all your smda reports of your samples. `malpedia_path` has to point to a folder containing your dumped samples. This folder is used to test the generated YARA rules (currently not working any more) against the malware you provided for YARA rule generation. As well have `yaraBinary` and `yaracBinary` to point to the location of your YARA installation. The `output_path` shows the tool where to store your results.

```
  "skipSMDAInsertions": false,
  "skipUniqueNgramTableCreation": false,
  "skipYaraRuleGeneration": false,
  "skipRuleValidation": false,
```
Those flags can be used to disable certains steps, for example the complete insertion of your smda files into the database. This can be used if an error occured in a later step and you want to save time.

You can choose several rankers by writing just the name of the raanker defined in the ranking factory.
You can choose different prefilters at the wildcardConfig section based on the name of the prefilters.
They are always executed in the same order as written in the config file. If you want to create custom filters and rankers, the factory files should be a good entry point. The design is very modular, so your should be able to integrate your own filters very quick by just creating a new class, adding it to the factory class and compile again. A real plugin interface for external jar/class files is a TODO.

## Workflow
1. Get SMDA reports from your malware pool. (https://github.com/danielplohmann/smda)
2. Create a valid config file (set folders to smda reports and to your pool, etc).
3. Start postgresql daemon
4. Start capstone_server on port 12345
5. Launch yara-signator (`java -jar target/yara-signator-0.0.1-SNAPSHOT-jar-with-dependencies.jar >> logfile.txt`)
6. Monitor your log file: `tail -F logfile.txt`
7. Check if capstone_server crashed, if yes, restart it

## FAQ
Q: Why are there so many MongoDB classes if you do not even use a mongo database?<br />
A: This is currently a first commit, I left as much code as I had in the project. If someone is reading the BS thesis and wants to retrace the concept, the source code will be available at least in this commit.

Q: This capstone_server is a C program, why don't you use the capstone bindings for JAVA like everyone else?<br />
A: I had some strange memory leaks (>10GB lost memory) when running many millions of opcodes against capstone over some hours. I couldn't fix them in JAVA and so I created a small program in C which communicates over TCP with other processes. If this process would have any memory leaks, you could simply restart it and the memory would be released back to the OS.

## Report
The approach is described in detail in my BS thesis:
http://cocacoding.com/papers/Automatic_Generation_of_code_based_YARA_Signatures.pdf <br />
SHA512: <br /> 0384d6ec497cbfca2ec4a7739337088c8c859e86ca63339fd3d26c8be2176e3378210ac6cbd725d08a2f672e9cb2dcecc09eef2ec2dbc975de726b0b918795b2 <br />
SHA256: <br />
4f0530d0da48b394cb0798c434ffc70c33ae351e54c77454c87546d17ec52b60 <br />

This project was mainly developed during the writing of the previously mentioned BS thesis. According to this I would like to thank Daniel Plohmann for his great support and helpful ideas during his supervision and beyond, especially regarding to this software project.
