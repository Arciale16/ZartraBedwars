#!/usr/bin/env python3
"""Generate or verify the exact Java 8 M20 Proxy API checkpoint."""
from pathlib import Path
import argparse, sys
import api_compatibility
ROOT=Path(__file__).resolve().parents[2]
BASELINE=ROOT/"build/api-signature-baseline-m20-proxy-api.txt"
CLASS_ROOT=ROOT/"proxy/zbw-proxy-api/target/classes"
def observed():
    """Return deterministic Proxy API signatures with Java 8 enforcement."""
    if not CLASS_ROOT.is_dir(): raise ValueError("Missing M20 compiled classes")
    signatures=[];count=0
    for path in sorted(CLASS_ROOT.rglob("*.class")):
        lines=api_compatibility.signature(path,52)
        if lines: count+=1;signatures.extend(lines)
    if not count: raise ValueError("No M20 Proxy public classes found")
    return "\n".join(("# ZartraBedWars M20 Proxy Java 8 API baseline","# class-major=52",*sorted(signatures),""))
def main():
    """Generate or compare the immutable Proxy API checkpoint."""
    parser=argparse.ArgumentParser();parser.add_argument("command",choices=("generate","check"));command=parser.parse_args().command;current=observed()
    if command=="generate": BASELINE.write_text(current,encoding="utf-8",newline="\n");print("Generated M20 Proxy API baseline");return 0
    if not BASELINE.is_file() or BASELINE.read_text(encoding="utf-8")!=current: print("ERROR: M20 Proxy API differs from checkpoint",file=sys.stderr);return 1
    print("M20 Proxy API compatibility PASS");return 0
if __name__=="__main__": raise SystemExit(main())