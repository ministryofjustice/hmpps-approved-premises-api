#!/usr/bin/env python3
"""Collect integration test timings and allocate test files to CI shards."""

import argparse
import json
import math
import os
from pathlib import Path
import statistics
import sys
import xml.etree.ElementTree as ET


TEST_ROOT = Path("src/test/kotlin")


def discover_tests(root):
    return sorted(
        path.relative_to(root).with_suffix("").as_posix().replace("/", ".")
        for path in root.rglob("*.kt")
        if "integration" in path.relative_to(root).parts
        and (path.name.endswith("Test.kt") or path.name.endswith("IT.kt"))
    )


def collect_timings(report_dir):
    reports = sorted(report_dir.glob("TEST-*.xml"))
    if not reports:
        raise ValueError(f"No JUnit XML reports found in {report_dir}")

    timings = {}
    for report in reports:
        suite = ET.parse(report).getroot()
        if suite.tag != "testsuite":
            raise ValueError(f"Unexpected JUnit report root in {report}: {suite.tag}")
        name = suite.attrib["name"].split("$", 1)[0]
        seconds = float(suite.attrib["time"])
        if not math.isfinite(seconds) or seconds < 0:
            raise ValueError(f"Invalid test duration in {report}: {seconds}")
        timings[name] = timings.get(name, 0) + seconds
    return timings


def load_timings(directory):
    timings = {}
    for path in sorted(directory.glob("*.json")):
        with path.open() as file:
            part = json.load(file)
        if not isinstance(part, dict):
            raise ValueError(f"Invalid timing data in {path}")
        for name, seconds in part.items():
            if not isinstance(name, str) or isinstance(seconds, bool) or not isinstance(seconds, (int, float)) or not math.isfinite(seconds) or seconds < 0:
                raise ValueError(f"Invalid timing entry in {path}: {name}={seconds}")
            if name in timings:
                raise ValueError(f"Duplicate timing entry for {name} in {path}")
            timings[name] = seconds
    return timings


def allocate(tests, timings, shard_count):
    if shard_count < 1 or len(tests) < shard_count:
        raise ValueError("The number of shards must be positive and no greater than the number of test files")
    if len(set(tests)) != len(tests):
        raise ValueError("Duplicate integration test class names")

    shards = [[] for _ in range(shard_count)]
    known = [timings[test] for test in tests if test in timings]
    if not known:
        for index, test in enumerate(tests):
            shards[index % shard_count].append(test)
        return shards

    default = statistics.median(known)
    weights = {test: timings.get(test, default) for test in tests}
    totals = [0.0] * shard_count
    for test in sorted(tests, key=lambda test: (-weights[test], test)):
        index = min(range(shard_count), key=lambda i: (totals[i], len(shards[i]), i))
        shards[index].append(test)
        totals[index] += weights[test]
    return shards


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    split = commands.add_parser("split")
    split.add_argument("--shards", type=int, required=True)
    split.add_argument("--timings-dir", type=Path, required=True)
    collect = commands.add_parser("collect")
    collect.add_argument("report_dir", type=Path)
    collect.add_argument("output", type=Path)
    args = parser.parse_args()

    if args.command == "collect":
        args.output.write_text(json.dumps(collect_timings(args.report_dir), sort_keys=True) + "\n")
        return

    tests = discover_tests(TEST_ROOT)
    timings = load_timings(args.timings_dir)
    shards = allocate(tests, timings, args.shards)
    known = sum(test in timings for test in tests)
    print(f"Allocating {len(tests)} test files across {args.shards} shards; {known} have historical timings")
    for index, shard in enumerate(shards):
        print(f"Shard {index + 1}: {len(shard)} files")
    matrix = [{"index": index, "number": index + 1} for index in range(args.shards)]
    with open(os.environ["GITHUB_OUTPUT"], "a") as output:
        output.write(f"matrix={json.dumps(matrix, separators=(',', ':'))}\n")
        output.write(f"shards={json.dumps(shards, separators=(',', ':'))}\n")


if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError, KeyError, ET.ParseError) as error:
        print(f"Integration test sharding failed: {error}", file=sys.stderr)
        sys.exit(1)
