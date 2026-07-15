#!/usr/bin/env python3
"""Validate, verify and expose the immutable M04 database image lock."""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import sys
import urllib.request


ROOT = Path(__file__).resolve().parents[2]
LOCK = ROOT / "build" / "m04-database-container-lock.json"
DIGEST = re.compile(r"sha256:[0-9a-f]{64}")
REFERENCE = re.compile(
    r"docker\.io/library/(?P<engine>mysql|mariadb):(?P<version>[0-9]+(?:\.[0-9]+)+)@"
    r"(?P<digest>sha256:[0-9a-f]{64})")


def load_lock() -> dict[str, object]:
    return json.loads(LOCK.read_text(encoding="utf-8"))


def by_engine(lock: dict[str, object], engine: str) -> dict[str, object]:
    matches = [image for image in lock["images"] if image["engine"] == engine]
    if len(matches) != 1:
        raise ValueError(f"expected one locked {engine} image, found {len(matches)}")
    return matches[0]


def validate(lock: dict[str, object]) -> list[str]:
    errors: list[str] = []
    if lock.get("schema_version") != 1:
        errors.append("database image lock schema must be 1")
    policy = lock.get("policy", {})
    expected_policy = {
        "allowed_platform": "linux/amd64",
        "ci_pull_only": True,
        "digest_required": True,
        "dynamic_tags_allowed": False,
        "product_bundling_allowed": False,
        "redistribution_allowed": False,
    }
    if policy != expected_policy:
        errors.append("database image policy drifted")
    images = lock.get("images", [])
    if {image.get("engine") for image in images} != {"mysql", "mariadb"} or len(images) != 2:
        errors.append("lock must contain exactly one MySQL and one MariaDB image")
    for image in images:
        engine = str(image.get("engine", ""))
        match = REFERENCE.fullmatch(str(image.get("reference", "")))
        if not match:
            errors.append(f"{engine}: reference is not an exact official tag@digest")
        elif (match.group("engine") != engine or match.group("version") != image.get("version")
              or match.group("digest") != image.get("index_digest")):
            errors.append(f"{engine}: reference, version and index digest disagree")
        for field in ("index_digest", "linux_amd64_manifest_digest", "config_digest"):
            if not DIGEST.fullmatch(str(image.get(field, ""))):
                errors.append(f"{engine}: invalid {field}")
        if not re.fullmatch(r"[0-9a-f]{40}", str(image.get("source_commit", ""))):
            errors.append(f"{engine}: source commit is not immutable")
        for evidence_name in ("source_manifest", "license"):
            evidence = image.get(evidence_name, {})
            if not str(evidence.get("url", "")).startswith("https://raw.githubusercontent.com/"):
                errors.append(f"{engine}: {evidence_name} is not an immutable GitHub source")
            if not re.fullmatch(r"[0-9a-f]{64}", str(evidence.get("sha256", ""))):
                errors.append(f"{engine}: {evidence_name} lacks a SHA-256 lock")
        if image.get("license", {}).get("spdx") != "GPL-2.0-only":
            errors.append(f"{engine}: unexpected image licence")
        rights = image.get("rights", {})
        if (rights.get("scope") != "CI-ONLY" or rights.get("product_bundled")
                or rights.get("redistribution") or rights.get("modification")
                or not rights.get("commercial_use")):
            errors.append(f"{engine}: image rights violate the CI-only policy")
    return errors


def fetch_verified(url: str, expected: str) -> bytes:
    request = urllib.request.Request(url, headers={"User-Agent": "ZartraBedWars-RC077/1"})
    with urllib.request.urlopen(request, timeout=60) as response:
        content = response.read()
    observed = hashlib.sha256(content).hexdigest()
    if observed != expected:
        raise ValueError(f"immutable provenance checksum mismatch for {url}: {observed}")
    return content


def verify_provenance(image: dict[str, object]) -> None:
    manifest_record = image["source_manifest"]
    manifest = json.loads(fetch_verified(
        str(manifest_record["url"]), str(manifest_record["sha256"])))
    version = str(image["version"])
    if not any(isinstance(value, dict) and value.get("version") == version
               for value in manifest.values()):
        raise ValueError(f"source manifest does not declare {image['engine']} {version}")
    license_record = image["license"]
    license_text = fetch_verified(str(license_record["url"]), str(license_record["sha256"]))
    if b"GNU GENERAL PUBLIC LICENSE" not in license_text.upper():
        raise ValueError(f"{image['engine']} immutable licence evidence is not GPL text")


def verify_local(image: dict[str, object]) -> None:
    command = [
        "docker", "image", "inspect", str(image["reference"]),
        "--format", "{{json .RepoDigests}}|{{.Os}}|{{.Architecture}}",
    ]
    result = subprocess.run(command, check=False, capture_output=True, text=True)
    if result.returncode:
        raise ValueError(f"docker image inspection failed for {image['engine']}")
    digest_json, operating_system, architecture = result.stdout.strip().split("|", 2)
    repository_digests = json.loads(digest_json)
    expected_suffix = "@" + str(image["index_digest"])
    if not any(value.endswith(expected_suffix) for value in repository_digests):
        raise ValueError(f"pulled {image['engine']} image does not match the locked index digest")
    if operating_system != "linux" or architecture != "amd64":
        raise ValueError(
            f"{image['engine']} image platform is {operating_system}/{architecture}, expected linux/amd64")


def main() -> int:
    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers(dest="command", required=True)
    subparsers.add_parser("validate")
    for command in ("reference", "version", "verify-provenance", "verify-local"):
        child = subparsers.add_parser(command)
        child.add_argument("--engine", choices=("mysql", "mariadb"), required=True)
    arguments = parser.parse_args()
    lock = load_lock()
    errors = validate(lock)
    if errors:
        for error in errors:
            print(f"ERROR: {error}", file=sys.stderr)
        return 1
    if arguments.command == "validate":
        print("M04 database image lock PASS: two official CI-only images, exact tags/digests and licence provenance.")
        return 0
    image = by_engine(lock, arguments.engine)
    if arguments.command == "reference":
        print(image["reference"])
    elif arguments.command == "version":
        print(image["version"])
    elif arguments.command == "verify-provenance":
        verify_provenance(image)
        print(f"{arguments.engine} immutable source and GPL-2.0-only evidence PASS.")
    else:
        verify_local(image)
        print(f"{arguments.engine} pulled digest and linux/amd64 platform PASS.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
