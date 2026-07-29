#!/usr/bin/env python3
"""Generate or verify exact M20 Proxy API and adapter checkpoints."""
from pathlib import Path
import argparse, sys
import api_compatibility
ROOT=Path(__file__).resolve().parents[2]
BASELINE=ROOT/"build/api-signature-baseline-m20-proxy-api.txt"
ROOTS=(("proxy/zbw-proxy-api/target/classes",52),("proxy/zbw-bungeecord/target/classes",52),("proxy/zbw-velocity/target/classes",65))
def observed():
    """Return deterministic Proxy signatures with Java 8/21 enforcement."""
    signatures=[];count=0
    for relative,major in ROOTS:
        class_root=ROOT/relative
        if not class_root.is_dir(): raise ValueError(f"Missing M20 compiled classes: {relative}")
        for path in sorted(class_root.rglob("*.class")):
            lines=api_compatibility.signature(path,major)
            if lines: count+=1;signatures.extend(lines)
    if not count: raise ValueError("No M20 Proxy public classes found")
    return "\n".join(("# ZartraBedWars M20 Proxy API/adapter baseline","# class-major=52/65",*sorted(signatures),""))
def main():
    """Generate or compare the immutable M20 Proxy checkpoint."""
    parser=argparse.ArgumentParser();parser.add_argument("command",choices=("generate","check"));command=parser.parse_args().command;current=observed()
    if command=="generate": BASELINE.write_text(current,encoding="utf-8",newline="\n");print("Generated M20 Proxy API/adapter baseline");return 0
    if not BASELINE.is_file() or BASELINE.read_text(encoding="utf-8")!=current: print("ERROR: M20 Proxy API/adapters differ from checkpoint",file=sys.stderr);return 1
    print("M20 Proxy API/adapter compatibility PASS");return 0
if __name__=="__main__": raise SystemExit(main())
