#!/usr/bin/env python3
"""Generate or verify the exact Java 8 M19 Redis API checkpoint."""
from pathlib import Path
import argparse, sys
import api_compatibility
ROOT=Path(__file__).resolve().parents[2]
BASELINE=ROOT/"build/api-signature-baseline-m19-redis-api.txt"
CLASS_ROOT=ROOT/"redis/zbw-redis-api/target/classes"
def observed():
    """Return deterministic Redis API signatures with Java 8 enforcement."""
    if not CLASS_ROOT.is_dir(): raise ValueError("Missing M19 compiled classes")
    signatures=[]; count=0
    for path in sorted(CLASS_ROOT.rglob("*.class")):
        lines=api_compatibility.signature(path,52)
        if lines: count+=1; signatures.extend(lines)
    if not count: raise ValueError("No M19 Redis public classes found")
    return "\n".join(("# ZartraBedWars M19 Redis Java 8 API baseline","# class-major=52",*sorted(signatures),""))
def main():
    """Generate or compare the immutable Redis API checkpoint."""
    parser=argparse.ArgumentParser();parser.add_argument("command",choices=("generate","check"));command=parser.parse_args().command;current=observed()
    if command=="generate": BASELINE.write_text(current,encoding="utf-8",newline="\n");print("Generated M19 Redis API baseline");return 0
    if not BASELINE.is_file() or BASELINE.read_text(encoding="utf-8")!=current: print("ERROR: M19 Redis API differs from checkpoint",file=sys.stderr);return 1
    print("M19 Redis API compatibility PASS");return 0
if __name__=="__main__": raise SystemExit(main())