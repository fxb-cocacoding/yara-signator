#!/bin/sh

# Build the datastore folders
mkdir malpedia
mkdir goodware
mkdir smda_report_output
mkdir yara-output

# Get the goodware test suite
cd goodware
git clone https://github.com/danielplohmann/empty_msvc.git
cd ..

unzip -P infected malpedia.zip

python -m venv venv
source venv/bin/activate
pip install smda
pip install tqdm
python disassemble_benign.py smda_report_output
python analyze_malpedia.py malpedia smda_report_output
