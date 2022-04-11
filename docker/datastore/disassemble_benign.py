import hashlib
import json
import os
import re
import sys
import logging
from multiprocessing import Pool, cpu_count
import traceback

import tqdm

from smda.Disassembler import Disassembler


def ensure_dir(filepath):
    try:
        os.makedirs(filepath)
        time.sleep(0.2)
    except:
        pass


def isBuffer(filepath):
    if "/buffer_x64/" in filepath or "/buffer_x86/" in filepath:
        return True
    return False


def getBitnessFromFilepath(filepath):
    if "x64/" in filepath or "_x64_" in filepath:
        return 64
    return 32


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


def work(input_element):
    REPORT = None
    INPUT_FILEPATH = input_element['filepath']
    INPUT_FILENAME = input_element['filename']
    INPUT_ROOT_PATH = input_element['root_path']
    OUTPUT_PATH = input_element['output_path']
    BUFFER = readFileContent(INPUT_FILEPATH)
    INPUT_SHA256 = hashlib.sha256(BUFFER).hexdigest()
    # skip previously analyzed files
    if INPUT_SHA256 + ".smda" in input_element['finished_reports']:
        print("Skipping file {}".format(filepath))
        return
    # analyze file
    try:
        disassembler = Disassembler()
        print("Analyzing file: {}".format(INPUT_FILEPATH))
        if isBuffer(INPUT_FILEPATH):
            BITNESS = getBitnessFromFilepath(INPUT_FILEPATH)
            REPORT = disassembler.disassembleBuffer(BUFFER, 0x0, BITNESS)
        else:
            REPORT = disassembler.disassembleFile(INPUT_FILEPATH)
        if REPORT:
            if "msvc" in INPUT_FILEPATH:
                REPORT.family = "msvc"
            else:
                REPORT.family = "benign"
            REPORT.version = ""
            REPORT.filename = INPUT_FILENAME
            with open(OUTPUT_PATH + os.sep + INPUT_SHA256 + ".smda", "w") as fout:
                json.dump(REPORT.toDict(), fout, indent=1, sort_keys=True)
                logger.info("Wrote " + str(OUTPUT_PATH + os.sep + INPUT_SHA256) + ".smda (" + INPUT_FILENAME + ")")
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

    if len(sys.argv) < 2:
        print("usage: %s <smda_report_root>" % sys.argv[0])
        sys.exit(1)
    binary_root_path = str(os.path.dirname(os.path.abspath(__file__)) + os.sep + "goodware")
    print(binary_root_path)
    output_path = sys.argv[1]
    ensure_dir(output_path)
    finished_reports = getAllReportFilenames(output_path)
    input_queue = []
    # Find all targets (everything) to disassemble in malpedia.
    for root, subdir, files in sorted(os.walk(binary_root_path)):
        for filename in sorted(files):
            filepath = root + os.sep + filename
            input_element = {
                "filename": filename,
                "finished_reports": finished_reports,
                "filepath": filepath,
                "output_path": output_path,
                "root_path": binary_root_path
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

