# Docker

Files are used to test your setup
Place the following content into the datastore folder:

```
.
├── VERSIONING.txt
├── malpedia
├── smda_report_output
└── yara-output
```

### VERSIONING.txt

This file is used internally by YARA-Signator to build comments into the YARA signatures that are generated.
You can customize them by using e.g. data from git of your own malware repository.
For the YARA signatures we generate regularly we use the commit history from Malpedia.

### malpedia

This folder contains the malware repository we want to consume.
It requires labeled data so we need a specific order of the folders: ./malpedia/win.<family_name>/<sample_name>
Between family name and sample name different sub folders are allowed and won't be recognized.
This allows you to keep version etc. of a specific malware family.

We have this structure for our test data (the first 4096 bytes of the files are wiped so you cannot execute them, this is only done to publish them):

```
.
├── malpedia
│   ├── win.citadel
│   │   ├── 2012-06-15-1.3.4.5
│   │   │   └── 0e967868c1f693097857d6d1069a3efca1e50f4516bb2637a10761d9bf4992ff_unpacked
│   │   ├── 2012-11-08-1.3.5.1
│   │   │   └── bc642f33c7dfc74e1a4bda2b237aff0ef3e6ac7ca660d7053583f5bd911c9aea_unpacked
│   │   └── 2014-01-23-1.3.4.0
│   │       └── bb13594ee346f3c55abdccf97138f094b88486e5e6b824da635af599e08b0aba_unpacked
│   ├── win.dridex
│   │   └── 2016-04-19
│   │       └── 212a9f051aef1d0b51f9c1eabcf28b96239fdc18f72e202adfac4410617b2cfe_unpacked
│   └── win.urlzone
│       └── 2015-12-21
│           └── 15896a44319d18f8486561b078146c30a0ce1cd7e6038f6d614324a39dfc6c28_unpacked
```

We have an encrypted folder as malware repository for the tests, the password is infected.
This way we prevent that AV software is triggered when moving the git repository.
To work with the data you need a (ideally virtual) machine without AV.

Important note: The folder must be called `malpedia`, unfortunately some parts in the software are still hardcoded for this folder name.
Do not rename the folder.
Also we need the format <platform>.<family_name> as we parse the folder names.

