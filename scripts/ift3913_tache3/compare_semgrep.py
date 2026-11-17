"""Fail CI when a Semgrep report contains findings absent from the baseline."""

import argparse
import json
import sys
from collections import Counter
from pathlib import Path


def load_report(path: Path) -> list[dict]:
    report = json.loads(path.read_text(encoding="utf-8"))
    if report.get("errors"):
        raise ValueError(f"Semgrep reported scan errors in {path}: {report['errors']}")
    return report["results"]


def identity(finding: dict) -> tuple[str, str, int, int]:
    start = finding["start"]
    return (
        finding["check_id"],
        finding["path"].replace("\\", "/"),
        start["line"],
        start["col"],
    )


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("baseline", type=Path)
    parser.add_argument("current", type=Path)
    args = parser.parse_args()

    try:
        before = load_report(args.baseline)
        after = load_report(args.current)
    except (OSError, KeyError, ValueError) as exc:
        print(f"Cannot compare Semgrep reports: {exc}", file=sys.stderr)
        return 2

    new = Counter(map(identity, after)) - Counter(map(identity, before))
    print(f"Semgrep findings: baseline={len(before)}, current={len(after)}, new={sum(new.values())}")
    for (rule, path, line, col), count in sorted(new.items()):
        print(f"NEW {rule} {path}:{line}:{col} (count={count})")
    return 1 if new else 0


if __name__ == "__main__":
    sys.exit(main())
