#!/usr/bin/env python3
"""Check Surefire summaries before invoking the Tâche 3 Rickroll action."""

import argparse
import os
import re
from pathlib import Path


SUMMARY = re.compile(r"Tests run:\s*\d+,\s*Failures:\s*(\d+),\s*Errors:\s*(\d+)")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("reports", type=Path, help="Directory containing Surefire text summaries")
    args = parser.parse_args()

    reports = sorted(args.reports.glob("*.txt"))
    failed = []
    for report in reports:
        summaries = SUMMARY.findall(report.read_text(encoding="utf-8", errors="replace"))
        if any(int(failures) or int(errors) for failures, errors in summaries):
            failed.append(report)

    print(f"Checked {len(reports)} Surefire summaries; failed test reports: {len(failed)}")
    for report in failed:
        print(f"FAILED TEST REPORT: {report}")

    output = os.environ.get("GITHUB_OUTPUT")
    if output:
        with Path(output).open("a", encoding="utf-8") as stream:
            stream.write(f"failed={'true' if failed else 'false'}\n")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
