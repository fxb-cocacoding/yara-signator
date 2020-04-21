#!/usr/bin/env python3

import sys
import re
import os
import subprocess
import glob
import json

from collections import Counter


def crawl_malpedia(malpedia_path):
    result = {
        "detectable": {},
        "all": {},
        "filemap": {}
    }
    dump_file_pattern = re.compile("dump7?_0x[0-9a-fA-F]{8,16}$")
    unpacked_file_pattern = re.compile("_unpacked(_x64)?$")
    for root, _, files in os.walk(malpedia_path):
        for filename in files:
            input_filepath = root + os.sep + filename
            # translate sample family path to rule format
            relative_path = input_filepath[input_filepath.find("malpedia"):]
            family = relative_path.split("/")[1].replace(".", "_")
            if ".git" in root or "yara" in root or "notes" in filename or ".json" in filename or "README.md" in filename:
                continue
            if family not in result["all"]:
                result["all"][family] = []
            if family not in result["detectable"]:
                result["detectable"][family] = []
            result["all"][family].append(filename)
            if "elf." in input_filepath and "x86" in input_filepath and re.search(unpacked_file_pattern, filename):
                result["detectable"][family].append(filename)
            elif "osx." in input_filepath and re.search(unpacked_file_pattern, filename):
                result["detectable"][family].append(filename)
            elif "win." in input_filepath and re.search(unpacked_file_pattern, filename):
                result["detectable"][family].append(filename)
            elif re.search(dump_file_pattern, filename):
                result["detectable"][family].append(filename)
            result["filemap"][filename] = input_filepath
    return result


def parse_lines(report):
    result = {}
    for line in report.split("\\n"):
        if not line or len(line.split(" ")) < 2:
            continue
        rule, sample = line.split(" ", 1)
        if ".git" in sample:
            continue
        family_by_rule = rule[:-5]
        family_by_rule = family_by_rule.replace("b'", "")
        # translate sample family path to rule format
        # sample_family = sample.split("/")[3].replace(".", "_")
        sample_filename = sample.split("/")[-1]
        if sample_filename not in result:
            result[sample_filename] = []
        result[sample_filename].append(family_by_rule)
    return result


def parse_scan(target_rule, report):
    result = []
    current_rule = ""
    for line in report.split("\\n"):
        if "_auto" in line:
            current_rule = line.split(" ")[0]
            current_rule = current_rule.replace("b'", "")
        if current_rule == target_rule and line.startswith("0x"):
            offset, sequence, match = line.split(":")
            result.append([sequence, offset, match.strip()])
    return result


def compile_yara(path):
    yara_filename = path.split("/")[-1] + ".yac"
    command = [str(YARAC_PATH)] + glob.glob(path + "/*/*/*/*") + [yara_filename]
    output = subprocess.check_output(command)
    print(output)
    return yara_filename


def run_yara(rule_path, malpedia_path):
    command = [str(YARA_PATH), rule_path, "-r", malpedia_path]
    return str(subprocess.check_output(command))


def run_yara_detailed(rule_path, malpedia_path):
    command = [str(YARA_PATH), rule_path, "-s", "-r", malpedia_path]
    return str(subprocess.check_output(command))


if len(sys.argv) != 6:
    print("usage: {} <yara_path> <yarac_path> <signator_folder> <malpedia_folder> <json_output_file>".format(sys.argv[0]))
    sys.exit(1)

YARA_PATH = os.path.abspath(sys.argv[1])
YARAC_PATH = os.path.abspath(sys.argv[2])
RULE_FOLDER = os.path.abspath(sys.argv[3])
MALPEDIA_PATH = os.path.abspath(sys.argv[4])
JSON_OUTPUT = os.path.abspath(sys.argv[5])

print("Evaluating \"{}\"".format(RULE_FOLDER))

malpedia_files = crawl_malpedia(MALPEDIA_PATH)
compiled_rule = compile_yara(RULE_FOLDER)
print("Produced rule: {}, now running YARA".format(compiled_rule))

output = run_yara(compiled_rule, MALPEDIA_PATH)
print("YARA output has {} lines.".format(len(output.split("\n"))))
rule_hits = parse_lines(output)
covered_families = [family.replace(".", "_") for family in os.listdir(RULE_FOLDER)]

n = 0
n_detectable = 0
no_fp_rules = 0
no_fn_rules = 0
clean_rules = 0
true_positives = 0
unexpected_true_positives = 0
true_negatives = 0
false_positives = 0
false_negatives = 0

fp_counts = {}
fp_samples = {}
fn_counts = Counter()

fn_samples = []

for family, samples in malpedia_files["all"].items():
    is_detectable_family = family in covered_families
    is_no_fp_rule = is_detectable_family
    is_no_fn_rule = is_detectable_family
    for sample in samples:
        is_detectable_file = family in covered_families and family in malpedia_files["detectable"] and sample in malpedia_files["detectable"][family]
        n += 1
        num_fps = 0
        is_unpacked_dumped = "unpacked" in sample or "_dump" in sample
        is_correctly_detected = False
        is_wrongly_detected = False
        is_detected = sample in rule_hits
        if sample in rule_hits:
            is_correctly_detected = family in rule_hits[sample]
            for rule_name in rule_hits[sample]:
                if rule_name != family:
                    num_fps += 1
                    if rule_name not in fp_counts:
                        fp_counts[rule_name] = {}
                        fp_samples[rule_name] = {}
                    if family not in fp_counts[rule_name]:
                        fp_counts[rule_name][family] = 0
                        fp_samples[rule_name][family] = []
                    fp_counts[rule_name][family] += 1
                    if sample not in fp_samples[rule_name][family]:
                        fp_samples[rule_name][family].append(sample)
            is_wrongly_detected = num_fps > 0
        if is_detectable_family:
            if not is_detectable_file:
                true_negatives += 1 if not is_correctly_detected else 0
                unexpected_true_positives += 1 if is_correctly_detected else 0
            else:
                n_detectable += 1
                true_positives += 1 if is_correctly_detected else 0
                if not is_correctly_detected:
                    false_negatives += 1
                    fn_counts[family] += 1
                    fn_samples.append(family + "," + sample)
                    is_no_fn_rule = False
        else:
            if not is_wrongly_detected:
                true_negatives += 1
        false_positives += num_fps
        if num_fps:
            is_no_fp_rule = False
    no_fp_rules += 1 if is_no_fp_rule else 0
    no_fn_rules += 1 if is_no_fn_rule else 0
    clean_rules += 1 if is_no_fn_rule and is_no_fp_rule else 0

print("+" * 10 + " Statistics " + "+" * 10)
print("Samples (all):             {}".format(n))
print("Samples (detectable):      {}".format(n_detectable))
print("Families:                  {}".format(len(malpedia_files["all"])))
print("------------------------------")
print("Families covered by rules: {}".format(len(covered_families)))
print("Rules without FPs:         {}".format(no_fp_rules))
print("Rules without FNs:         {}".format(no_fn_rules))
print("'Clean' Rules:             {}".format(clean_rules))
print("------------------------------")
print("True Positives:  {} (+{} more hits in correct family)".format(true_positives, unexpected_true_positives))
print("False Positives: {}".format(false_positives))
print("True Negatives:  {}".format(true_negatives))
print("False Negatives: {}".format(false_negatives))
print("------------------------------")
tpfp = true_positives + false_positives
tpfn = true_positives + false_negatives
ppv = 0
tpr = 0
if tpfp:
    ppv = 1.0 *   true_positives / tpfp
    print("PPV / Precision: {:5.3f}".format(ppv))
else:
    print("PPV / Precision: -")
if tpfn:
    tpr = 1.0 *   true_positives / tpfn
    print("TPR / Recall:    {:5.3f}".format(tpr))
else:
    print("TPR / Recall:    -")
if ppv or tpr:
    print("F1:              {:5.3f}".format(2.0 * ppv * tpr / (ppv + tpr)))

print("+" * 30 + " Analysis " + "+" * 30)
print("FPs (in total: {} rules on {} families with {} samples):".format(len(fp_counts), sum([len(rule[1]) for rule in fp_counts.items()]), false_positives))
for rule, rule_fps in sorted(fp_counts.items(), key=lambda x : sum(x[1].values()), reverse=True):
    print("{}: {} families, {} samples".format(rule, len(rule_fps.keys()), sum(rule_fps.values())))
print("+" * 70)
print("top20 FNs: (in total: {} families with {} samples)".format(len(fn_counts.keys()), sum([item[1] for item in fn_counts.most_common()])))
for item in fn_counts.most_common(20):
    print(item)

if False:
    print("+" * 10 + " All False Negatives " + "+" * 10)
    for fn_sample in sorted(fn_samples):
        print(fn_sample)

results = {
    "false_positives": {},
    "fp_sequence_stats": {},
    "false_negatives": {},
    "statistics": {
        "samples_all": n,
        "samples_detectable": n_detectable,
        "families": len(malpedia_files["all"]),
        "families_covered": len(covered_families),
        "rules_without_fp": no_fp_rules,
        "rules_without_fn": no_fn_rules,
        "clean_rules": clean_rules,
        "true_positives": true_positives,
        "true_positives_bonus": unexpected_true_positives,
        "false_positives": false_positives,
        "true_negatives": true_negatives,
        "false_negatives": false_negatives,
        "f1_precision": ppv,
        "f1_recall": tpr,
        "f1_score": 2.0 * ppv * tpr / (ppv + tpr),
    }
}
for sample in fn_samples:
    family, filename = sample.split(",")
    fns = results["false_negatives"].get(family, [])
    fns.append(filename)
    results["false_negatives"][family] = fns

print("*" * 100)
fp_samples_detailed = {}
fp_sequence_stats = Counter()
for rule, fp_families in fp_samples.items():
    fp_samples_detailed[rule] = {}
    for fp_family, samples in fp_families.items():
        replacement = {}
        for sample in samples:
            output = run_yara_detailed(compiled_rule, malpedia_files["filemap"][sample])
            parsed = parse_scan(rule + "_auto", output)
            replacement[sample] = parsed
            sequence_count = len(set([entry[0] for entry in parsed]))
            if sequence_count:
                fp_sequence_stats[sequence_count] += 1
        fp_samples_detailed[rule][fp_family] = replacement
results["false_positives"] = fp_samples_detailed
results["fp_sequence_stats"] = dict(fp_sequence_stats)

#with open(compiled_rule[:-4] + ".json", "w") as fjson:
with open(JSON_OUTPUT, "w") as fjson:
    json.dump(results, fjson, indent=1, sort_keys=True)
