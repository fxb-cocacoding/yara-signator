import json
import os
import re
import sys
import logging
from multiprocessing import Pool, cpu_count
import traceback

import tqdm

from smda.Disassembler import Disassembler

import os
import re
import sys
import json
import struct

from collections import defaultdict


def get_word(buffer, start):
    return _get_binary_data(buffer, start, 2)


def get_dword(buffer, start):
    return _get_binary_data(buffer, start, 4)


def get_qword(buffer, start):
    return _get_binary_data(buffer, start, 8)


def _get_binary_data(buffer, start, length):
    if length not in _unsigned_unpack_formats:
        raise RuntimeError("Unsupported data length")

    return struct.unpack(_unsigned_unpack_formats[length], buffer[start:start + length])[0]


_unsigned_unpack_formats = {
    2: "H",
    4: "I",
    8: "Q"
}

def get_pe_offset(content):
    if len(content) >= 0x40:
        pe_offset = get_word(content, 0x3c)
        return pe_offset
    raise RuntimeError("Buffer too small to extract PE offset (< 0x40)")


def check_bitness(content):
    bitness = None
    pe_offset = get_pe_offset(content)
    if pe_offset and len(content) >= pe_offset + 6:
        bitness = get_word(content, pe_offset + 4)
        bitness_map = {0x14c: 32, 0x8664: 64}
        bitness = bitness_map[bitness] if bitness in bitness_map else 0
    return bitness




def parseBaseAddrFromArgs(filename):
    baddr_match = re.search(re.compile("0x(?P<base_addr>[0-9a-fA-F]{8,16})"), filename)
    if baddr_match:
        return int(baddr_match.group("base_addr"), 16)
    return 0


def getBitnessFromFilename(filename):
    baddr_match = re.search(re.compile("0x(?P<base_addr>[0-9a-fA-F]{8,16})"), filename)
    if baddr_match:
        return 32 if len(baddr_match.group("base_addr")) == 8 else 64
    return 0


def readFileContent(file_path):
    file_content = b""
    with open(file_path, "rb") as fin:
        file_content = fin.read()
    return file_content


def getAllReportFilenames(output_path):
    report_filenames = set([])
    for root, subdir, files in os.walk(output_path):
        for filename in files:
            report_filenames.add(filename)
    return report_filenames


def getFamilyName(input_path):
    family_name = ""
    abs_path = os.path.abspath(input_path)
    for folder in abs_path.split("/")[::-1]:
        if folder == "malpedia":
            break
        family_name = folder
    return family_name

def getSampleVersion(input_path, family):
    family_name = ""
    abs_path = os.path.dirname(os.path.abspath(input_path))
    for folder in abs_path.split("/")[::-1]:
        if folder == family or folder == "modules":
            break
        family_name = folder
    return family_name


def getMalpediaFilePath(input_path):
    egg = 'malpedia/'
    abs_path = os.path.abspath(input_path)
    pos = abs_path.index(egg)
    malpedia_filepath = abs_path[pos + len(egg):]
    return malpedia_filepath


def work(input_element):
    if input_element['filename'] + ".smda" in input_element['finished_reports']:
        print("Skipping file {}".format(filepath))
        return
    REPORT = None
    INPUT_FILEPATH = input_element['filepath']
    INPUT_FILENAME = input_element['filename']
    OUTPUT_PATH = input_element['output_path']
    MALPEDIA_PATH = input_element['malpedia_path']
    malpedia_relative_path = getMalpediaFilePath(INPUT_FILEPATH)
    in_family_path = os.sep.join(malpedia_relative_path.split(os.sep)[1:])
    if in_family_path.startswith("module"):
        return
    disassembler = Disassembler()
    try:
        if "elf." in INPUT_FILEPATH and ("x86" in INPUT_FILEPATH or "x64" in INPUT_FILEPATH) and re.search(unpacked_file_pattern, input_element['filename']):
            print("Analyzing file: {}".format(INPUT_FILEPATH))
            try:
                REPORT = disassembler.disassembleFile(INPUT_FILEPATH)
            except AttributeError:
                logger.error("exception for: " + str(INPUT_FILENAME))
        elif "win." in INPUT_FILEPATH and re.search(unpacked_file_pattern, input_element['filename']):
            print("Analyzing file: {}".format(INPUT_FILEPATH))
            try:
                REPORT = disassembler.disassembleFile(INPUT_FILEPATH)
            except AttributeError:
                logger.error("AttributeError for: " + str(INPUT_FILENAME))
        elif re.search(dump_file_pattern, input_element['filename']):
            print("Analyzing file: {}".format(INPUT_FILEPATH))
            BUFFER = readFileContent(INPUT_FILEPATH)
            BASE_ADDR = parseBaseAddrFromArgs(INPUT_FILENAME)
            BITNESS = getBitnessFromFilename(INPUT_FILENAME)
            try:
                REPORT = disassembler.disassembleBuffer(BUFFER, BASE_ADDR, BITNESS)
            except AttributeError:
                logger.error("AttributeError for: " + str(INPUT_FILENAME))
        if REPORT:
            REPORT.family = getFamilyName(INPUT_FILEPATH)
            REPORT.version = getSampleVersion(INPUT_FILEPATH, REPORT.family)
            REPORT.filename = os.path.basename(malpedia_relative_path)
            with open(OUTPUT_PATH + os.sep + INPUT_FILENAME + ".smda", "w") as fout:
                json.dump(REPORT.toDict(), fout, indent=1, sort_keys=True)
                logger.info("Wrote " + str(OUTPUT_PATH + os.sep + INPUT_FILENAME) + ".smda")
    except Exception:
        print("RunTimeError, we skip!")
        print("smda: " + str( INPUT_FILENAME ))
        traceback.print_exc()
    return None


if __name__ == "__main__":

    logging.basicConfig(filename="/tmp/smda.log",
                        filemode='a',
                        format='[%(asctime)s:%(msecs)d] %(name)s %(levelname)s %(message)s',
                        datefmt='%Y-%m-%d %H:%M:%S',
                        level=logging.INFO)

    logger = logging.getLogger('smda-multithreaded')
    formatter = logging.Formatter('%(process)d - %(processName)s - %(threadName)s - %(asctime)s - %(name)s - %(levelname)s - %(message)s')

    # Add logger to stdout
    handler = logging.StreamHandler(sys.stdout)
    handler.setLevel(logging.INFO)
    handler.setFormatter(formatter)
    logger.addHandler(handler)

    if len(sys.argv) < 3:
        print("usage: %s <malpedia_root> <smda_report_root>" % sys.argv[0])
        sys.exit(1)
    malpedia_path = sys.argv[1]
    output_path = sys.argv[2]
    finished_reports = getAllReportFilenames(output_path)
    dump_file_pattern = re.compile("dump7?_0x[0-9a-fA-F]{8,16}")
    unpacked_file_pattern = re.compile("_unpacked(_x64)?$")
    input_queue = []
    # Find all targets (everything) to disassemble in malpedia.
    for root, subdir, files in sorted(os.walk(malpedia_path)):
        if ".git" in root:
            continue
        for filename in sorted(files):
            if not (re.search(unpacked_file_pattern, filename) or re.search(dump_file_pattern, filename)):
                continue
            filepath = root + os.sep + filename
            input_element = {
                "filename": filename,
                "finished_reports": finished_reports,
                "filepath": filepath,
                "output_path": output_path,
                "malpedia_path": malpedia_path
            }
            input_queue.append(input_element)
    # Enable Pooling for faster disassembling
    # TODO: Fix cpu_count and add niceness
    results = []
    # with Pool(cpu_count()) as pool:
    with Pool(cpu_count()) as pool:
        for result in tqdm.tqdm(pool.imap_unordered(work, input_queue), total=len(input_queue)):
            results.append([result])
    print("DONE, shutting down")
