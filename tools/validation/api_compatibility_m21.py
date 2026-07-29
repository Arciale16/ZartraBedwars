#!/usr/bin/env python3
"""Generate or verify the exact M21 provider and party checkpoint."""

from pathlib import Path
import argparse
import sys

import api_compatibility


ROOT = Path(__file__).resolve().parents[2]
BASELINE = ROOT / "build/api-signature-baseline-m21-party-provider.txt"
ROOTS = (
    "api/zbw-api/target/classes",
    "party/zbw-party/target/classes",
    "party/zbw-party-sql/target/classes",
    "integrations/vault/zbw-integration-vault/target/classes",
    "integrations/luckperms/zbw-integration-luckperms/target/classes",
    "integrations/citizens/zbw-integration-citizens/target/classes",
    "integrations/znpcs/zbw-integration-znpcsplus/target/classes",
    "integrations/hologram/zbw-integration-decentholograms/target/classes",
    "integrations/party/zbw-integration-alessiopdp/target/classes",
    "integrations/anticheat/zbw-integration-grim/target/classes",
    "integrations/anticheat/zbw-integration-vulcan/target/classes",
    "cloud/zbw-cloudnet/target/classes",
    "integrations/world/zbw-integration-world-providers/target/classes",
    "observability/zbw-observability/target/classes",
)


def observed() -> str:
    """Return deterministic Java 8 signatures for the M21 Phase 1 boundary."""
    signatures = []
    count = 0
    for relative in ROOTS:
        class_root = ROOT / relative
        if not class_root.is_dir():
            raise ValueError(f"Missing M21 compiled classes: {relative}")
        for path in sorted(class_root.rglob("*.class")):
            lines = api_compatibility.signature(path, 52)
            if lines:
                count += 1
                signatures.extend(lines)
    if not count:
        raise ValueError("No M21 provider/party/adapter public classes found")
    return "\n".join((
        "# ZartraBedWars M21 provider SPI/native party/adapter baseline",
        "# class-major=52",
        *sorted(signatures),
        "",
    ))


def main() -> int:
    """Generate or compare the immutable M21 Phase 1 checkpoint."""
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=("generate", "check"))
    command = parser.parse_args().command
    current = observed()
    if command == "generate":
        BASELINE.write_text(current, encoding="utf-8", newline="\n")
        print("Generated M21 provider SPI/native party/adapter API baseline")
        return 0
    if not BASELINE.is_file() or BASELINE.read_text(encoding="utf-8") != current:
        print("ERROR: M21 provider SPI/native party API differs from checkpoint",
              file=sys.stderr)
        return 1
    print("M21 provider SPI/native party/adapter API compatibility PASS")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
