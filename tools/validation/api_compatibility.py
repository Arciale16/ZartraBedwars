#!/usr/bin/env python3
"""Generate or verify additive prior compatibility and exact M06 JVM API baselines."""

from __future__ import annotations

import argparse
from pathlib import Path
import struct


ROOT = Path(__file__).resolve().parents[2]
M02_BASELINE = ROOT / "build" / "api-signature-baseline.txt"
M03_BASELINE = ROOT / "build" / "api-signature-baseline-m03.txt"
M04_BASELINE = ROOT / "build" / "api-signature-baseline-m04.txt"
M05_BASELINE = ROOT / "build" / "api-signature-baseline-m05.txt"
BASELINE = ROOT / "build" / "api-signature-baseline-m06.txt"
MODERN_BASELINE = ROOT / "build" / "api-signature-baseline-m06-modern.txt"
NEUTRAL_MODULES = (
    "api/zbw-api",
    "domain/zbw-domain",
    "application/zbw-application",
    "sdk/zbw-sdk",
    "integrations/discord/zbw-integration-discord-api",
    "configuration/zbw-config",
    "storage/zbw-storage-api",
    "storage/zbw-storage-sql",
    "observability/zbw-observability",
    "compatibility/zbw-compat-api",
    "world/zbw-world",
)
MODERN_MODULES = (
    "compatibility/zbw-compat-v1_20-v1_21",
    "platform/paper/zbw-paper-modern",
)
VISIBLE = 0x0001 | 0x0004


class ClassReader:
    """Minimal class-file reader for names and JVM descriptors."""

    def __init__(self, content: bytes) -> None:
        self.content = content
        self.position = 0

    def u1(self) -> int:
        value = self.content[self.position]
        self.position += 1
        return value

    def u2(self) -> int:
        value = struct.unpack_from(">H", self.content, self.position)[0]
        self.position += 2
        return value

    def u4(self) -> int:
        value = struct.unpack_from(">I", self.content, self.position)[0]
        self.position += 4
        return value

    def skip(self, length: int) -> None:
        self.position += length


def skip_attributes(reader: ClassReader) -> None:
    for _ in range(reader.u2()):
        reader.u2()
        reader.skip(reader.u4())


def signature(path: Path, expected_major: int) -> list[str]:
    reader = ClassReader(path.read_bytes())
    if reader.u4() != 0xCAFEBABE:
        raise ValueError(f"Not a JVM class file: {path}")
    reader.u2()
    major = reader.u2()
    if major != expected_major:
        raise ValueError(
            f"Class must use bytecode {expected_major}, found {major}: {path}")
    pool: list[object | None] = [None] * reader.u2()
    index = 1
    while index < len(pool):
        tag = reader.u1()
        if tag == 1:
            length = reader.u2()
            pool[index] = reader.content[reader.position:reader.position + length].decode("utf-8")
            reader.skip(length)
        elif tag in {3, 4}:
            reader.skip(4)
        elif tag in {5, 6}:
            reader.skip(8)
            index += 1
        elif tag in {7, 8, 16, 19, 20}:
            pool[index] = reader.u2()
        elif tag in {9, 10, 11, 12, 17, 18}:
            reader.skip(4)
        elif tag == 15:
            reader.skip(3)
        else:
            raise ValueError(f"Unsupported constant-pool tag {tag}: {path}")
        index += 1

    def utf8(pool_index: int) -> str:
        value = pool[pool_index]
        if not isinstance(value, str):
            raise ValueError(f"Expected UTF-8 constant at {pool_index}: {path}")
        return value

    def class_name(pool_index: int) -> str:
        value = pool[pool_index]
        if not isinstance(value, int):
            raise ValueError(f"Expected class constant at {pool_index}: {path}")
        return utf8(value).replace("/", ".")

    access = reader.u2()
    this_class = class_name(reader.u2())
    super_index = reader.u2()
    superclass = class_name(super_index) if super_index else "-"
    interfaces = [class_name(reader.u2()) for _ in range(reader.u2())]
    lines: list[str] = []
    if access & VISIBLE:
        lines.append(
            f"CLASS {this_class} access=0x{access:04x} extends={superclass} "
            f"implements={','.join(sorted(interfaces))}")
    for kind in ("FIELD", "METHOD"):
        for _ in range(reader.u2()):
            member_access = reader.u2()
            name = utf8(reader.u2())
            descriptor = utf8(reader.u2())
            if access & VISIBLE and member_access & VISIBLE and name != "<clinit>":
                lines.append(
                    f"{kind} {this_class} {name} {descriptor} access=0x{member_access:04x}")
            skip_attributes(reader)
    skip_attributes(reader)
    return lines


def observed(modules: tuple[str, ...], major: int, title: str) -> str:
    header = [f"# ZartraBedWars {title} JVM binary API baseline", f"# class-major={major}"]
    signatures: list[str] = []
    count = 0
    for module in modules:
        classes = ROOT / module / "target" / "classes"
        if not classes.is_dir():
            raise ValueError(f"Missing compiled classes for {module}; run the current build first")
        for path in sorted(classes.rglob("*.class")):
            class_lines = signature(path, major)
            if class_lines:
                count += 1
                signatures.extend(class_lines)
    if not count:
        raise ValueError("No public classes found")
    return "\n".join(header + sorted(signatures)) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=("generate", "check"))
    arguments = parser.parse_args()
    current = observed(NEUTRAL_MODULES, 52, "M06 neutral")
    modern = observed(MODERN_MODULES, 65, "M06 modern")
    if arguments.command == "generate":
        BASELINE.write_text(current, encoding="utf-8")
        MODERN_BASELINE.write_text(modern, encoding="utf-8")
        print(
            "Generated M06 binary API baselines with "
            f"{current.count('CLASS ')} neutral and {modern.count('CLASS ')} modern public classes.")
        return 0
    if not BASELINE.is_file() or BASELINE.read_text(encoding="utf-8") != current:
        print("ERROR: M06 neutral binary API differs from its exact baseline")
        return 1
    if not MODERN_BASELINE.is_file() or MODERN_BASELINE.read_text(encoding="utf-8") != modern:
        print("ERROR: M06 modern binary API differs from its exact baseline")
        return 1
    for previous in (M02_BASELINE, M03_BASELINE, M04_BASELINE, M05_BASELINE):
        if not previous.is_file():
            print(f"ERROR: immutable prior binary API baseline is missing: {previous.name}")
            return 1
    current_lines = set(current.splitlines())
    missing = []
    for previous in (M02_BASELINE, M03_BASELINE, M04_BASELINE, M05_BASELINE):
        missing.extend(line for line in previous.read_text(encoding="utf-8").splitlines()
                       if line and not line.startswith("#") and line not in current_lines)
    if missing:
        print(f"ERROR: {len(missing)} prior binary signatures were removed or changed")
        return 1
    print(
        "Binary/API compatibility PASS: "
        f"{current.count('CLASS ')} Java 8 neutral and {modern.count('CLASS ')} Java 21 modern "
        "public classes; M02-M05 baselines preserved.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
