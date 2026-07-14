#!/usr/bin/env python3
"""Acquire, lock, inventory and validate all M1 build/CI artifacts."""

from __future__ import annotations

import argparse
import hashlib
import io
import json
from pathlib import Path
import re
import shutil
import sys
import tarfile
import tempfile
import urllib.request
import uuid
import xml.etree.ElementTree as ET
import zipfile


ROOT = Path(__file__).resolve().parents[2]
ACQUISITION = ROOT / "build" / "dependency-acquisition.json"
LOCK = ROOT / "build" / "dependency-lock.json"
SBOM = ROOT / "build" / "sbom.cdx.json"
BUILD_NOTICES = ROOT / "build" / "THIRD_PARTY_BUILD_NOTICES.md"
EXCLUDED_PARTS = {".git", ".tools", ".m2", "target"}
MAVEN_NAMESPACE = "http://maven.apache.org/POM/4.0.0"


def canonical_json(value: object) -> str:
    return json.dumps(value, indent=2, sort_keys=True, ensure_ascii=False) + "\n"


def sha256_bytes(value: bytes) -> str:
    return hashlib.sha256(value).hexdigest()


def hash_file(path: Path, algorithm: str) -> str:
    digest = hashlib.new(algorithm)
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def fetch(url: str, destination: Path) -> None:
    destination.parent.mkdir(parents=True, exist_ok=True)
    request = urllib.request.Request(url, headers={"User-Agent": "ZartraBedWars-M01/1"})
    with urllib.request.urlopen(request, timeout=90) as response:
        with tempfile.NamedTemporaryFile(dir=destination.parent, delete=False) as temporary:
            temporary_path = Path(temporary.name)
            shutil.copyfileobj(response, temporary, length=1024 * 1024)
    temporary_path.replace(destination)


def archive_member(path: Path, member_name: str) -> bytes:
    if path.name.endswith(".zip"):
        with zipfile.ZipFile(path) as archive:
            return archive.read(member_name)
    with tarfile.open(path, "r:gz") as archive:
        member = archive.extractfile(member_name)
        if member is None:
            raise ValueError(f"missing archive member {member_name} in {path.name}")
        return member.read()


def cache_name(artifact: dict[str, object]) -> str:
    explicit = artifact.get("cache_filename")
    if explicit:
        return str(explicit)
    return str(artifact["source"]).rsplit("/", 1)[-1]


def acquire(artifact: dict[str, object], cache: Path) -> dict[str, object]:
    result = dict(artifact)
    kind = str(artifact["kind"])
    if kind.endswith("ARCHIVE"):
        path = cache / cache_name(artifact)
        if not path.is_file():
            fetch(str(artifact["source"]), path)
        observed_sha256 = hash_file(path, "sha256")
        if observed_sha256 != artifact["sha256"]:
            raise ValueError(f"{artifact['id']} sha256 mismatch: {observed_sha256}")
        result["observed_sha256"] = observed_sha256
        if "sha512" in artifact:
            observed_sha512 = hash_file(path, "sha512")
            if observed_sha512 != artifact["sha512"]:
                raise ValueError(f"{artifact['id']} sha512 mismatch: {observed_sha512}")
            result["observed_sha512"] = observed_sha512
    elif kind == "CI_ACTION_GIT_COMMIT":
        match = re.fullmatch(r"https://github\.com/([^/]+)/([^/]+)/tree/([0-9a-f]{40})", str(artifact["source"]))
        if not match:
            raise ValueError(f"invalid immutable GitHub action source for {artifact['id']}")
        owner, repository, commit = match.groups()
        request = urllib.request.Request(
            f"https://api.github.com/repos/{owner}/{repository}/commits/{commit}",
            headers={"Accept": "application/vnd.github+json", "User-Agent": "ZartraBedWars-M01/1"},
        )
        with urllib.request.urlopen(request, timeout=30) as response:
            resolved = json.load(response)["sha"]
        if resolved != artifact["git_commit"]:
            raise ValueError(f"Git commit mismatch for {artifact['id']}: {resolved}")
        result["observed_git_commit"] = resolved
    license_record = dict(artifact["license"])
    if "path_in_archive" in license_record:
        path = cache / cache_name(artifact)
        license_bytes = archive_member(path, str(license_record["path_in_archive"]))
    else:
        license_cache = cache / (re.sub(r"[^a-zA-Z0-9_.-]", "_", str(artifact["id"])) + "-LICENSE")
        if not license_cache.is_file():
            fetch(str(license_record["source"]), license_cache)
        license_bytes = license_cache.read_bytes()
    observed_license = sha256_bytes(license_bytes)
    if observed_license != license_record["sha256"]:
        raise ValueError(f"{artifact['id']} license hash mismatch: {observed_license}")
    license_record["observed_sha256"] = observed_license
    if "assembly_exception_sha256" in license_record:
        if "path_in_archive" not in license_record:
            raise ValueError(f"{artifact['id']} assembly exception requires an archive licence path")
        assembly_path = str(Path(str(license_record["path_in_archive"])).parent / "ASSEMBLY_EXCEPTION").replace("\\", "/")
        assembly_bytes = archive_member(cache / cache_name(artifact), assembly_path)
        observed_assembly = sha256_bytes(assembly_bytes)
        if observed_assembly != license_record["assembly_exception_sha256"]:
            raise ValueError(f"{artifact['id']} assembly exception hash mismatch: {observed_assembly}")
        license_record["observed_assembly_exception_sha256"] = observed_assembly
    result["license"] = license_record
    if "notice" in artifact:
        notice = dict(artifact["notice"])
        notice_bytes = archive_member(cache / cache_name(artifact), str(notice["path_in_archive"]))
        observed_notice = sha256_bytes(notice_bytes)
        if observed_notice != notice["sha256"]:
            raise ValueError(f"{artifact['id']} notice hash mismatch: {observed_notice}")
        notice["observed_sha256"] = observed_notice
        result["notice"] = notice
    result["verification"] = "VERIFIED"
    return result


def make_lock(acquisition: dict[str, object], artifacts: list[dict[str, object]]) -> dict[str, object]:
    return {
        "schema_version": 1,
        "baseline_date": "2026-07-14",
        "policy": acquisition["policy"],
        "artifacts": sorted(artifacts, key=lambda item: str(item["id"])),
    }


def make_sbom(lock: dict[str, object]) -> dict[str, object]:
    lock_digest = sha256_bytes(canonical_json(lock).encode("utf-8"))
    components = []
    for artifact in lock["artifacts"]:
        identifier = str(artifact["id"])
        component = {
            "type": "application" if str(artifact["kind"]).startswith("CI_ACTION") else "framework",
            "bom-ref": identifier,
            "name": artifact["name"],
            "version": artifact["version"],
            "licenses": [{"license": {"id": artifact["license"]["spdx"]}}],
            "externalReferences": [{"type": "distribution", "url": artifact["source"]}],
            "properties": [
                {"name": "zartra.scope", "value": artifact["rights"]["scope"]},
                {"name": "zartra.productBundled", "value": "false"},
                {"name": "zartra.commercialUse", "value": str(artifact["rights"]["commercial_use"]).lower()},
            ],
        }
        if "sha256" in artifact:
            component["hashes"] = [{"alg": "SHA-256", "content": artifact["sha256"]}]
        components.append(component)
    return {
        "bomFormat": "CycloneDX",
        "specVersion": "1.6",
        "serialNumber": f"urn:uuid:{uuid.UUID(lock_digest[:32])}",
        "version": 1,
        "metadata": {"component": {"type": "application", "name": "ZartraBedWars M01 build governance", "version": "0.1.0-SNAPSHOT"}},
        "components": components,
    }


def make_notices(lock: dict[str, object]) -> str:
    lines = [
        "# Milestone 1 Build and CI Tool Notices",
        "",
        "These tools are used only to build or validate the project. They are not bundled in a ZartraBedWars product artifact.",
        "",
        "| Tool | Exact version/identity | Licence | Scope | Source |",
        "|---|---|---|---|---|",
    ]
    for artifact in lock["artifacts"]:
        lines.append(
            f"| {artifact['name']} | `{artifact['version']}` | {artifact['license']['spdx']} | {artifact['rights']['scope']} | {artifact['source']} |"
        )
    lines.extend(["", "Generated deterministically from `build/dependency-lock.json`. Product redistribution: none.", ""])
    return "\n".join(lines)


def validate_schema(acquisition: dict[str, object], lock: dict[str, object]) -> list[str]:
    errors: list[str] = []
    expected = {artifact["id"]: artifact for artifact in acquisition["artifacts"]}
    observed = {artifact["id"]: artifact for artifact in lock["artifacts"]}
    if len(expected) != len(acquisition["artifacts"]):
        errors.append("duplicate artifact ID in acquisition manifest")
    if len(observed) != len(lock["artifacts"]):
        errors.append("duplicate artifact ID in dependency lock")
    if expected.keys() != observed.keys():
        errors.append("acquisition manifest and dependency lock artifact IDs differ")
    for identifier, source in expected.items():
        target = observed.get(identifier, {})
        for field in ("name", "version", "kind", "source", "sha256", "sha512", "git_commit", "rights"):
            if field in source and target.get(field) != source[field]:
                errors.append(f"{identifier}: locked {field} differs from acquisition manifest")
        version = str(source["version"])
        if any(token in version.upper() for token in ("LATEST", "RELEASE", "SNAPSHOT", "[", "]", "(", ")")):
            errors.append(f"{identifier}: dynamic/range/SNAPSHOT version prohibited")
        rights = source.get("rights", {})
        for field in ("scope", "redistribute_product", "shade_product", "modify", "commercial_use", "attribution"):
            if field not in rights:
                errors.append(f"{identifier}: missing rights field {field}")
        if rights.get("redistribute_product") or rights.get("shade_product"):
            errors.append(f"{identifier}: M1 build/CI artifact may not enter product packaging")
        if target.get("verification") != "VERIFIED":
            errors.append(f"{identifier}: verification is not VERIFIED")
        if "sha256" in source and target.get("observed_sha256") != source["sha256"]:
            errors.append(f"{identifier}: observed sha256 missing or mismatched")
        if "git_commit" in source and target.get("observed_git_commit") != source["git_commit"]:
            errors.append(f"{identifier}: observed git commit missing or mismatched")
        if target.get("license", {}).get("observed_sha256") != source["license"]["sha256"]:
            errors.append(f"{identifier}: observed licence hash missing or mismatched")
        expected_assembly = source["license"].get("assembly_exception_sha256")
        if expected_assembly and target.get("license", {}).get("observed_assembly_exception_sha256") != expected_assembly:
            errors.append(f"{identifier}: observed assembly-exception hash missing or mismatched")
    return errors


def validate_poms(lock: dict[str, object]) -> list[str]:
    errors: list[str] = []
    locked_maven = {artifact["id"] for artifact in lock["artifacts"] if str(artifact["id"]).startswith("maven:")}
    namespace = {"m": MAVEN_NAMESPACE}
    for pom in ROOT.rglob("pom.xml"):
        if any(part in EXCLUDED_PARTS for part in pom.parts):
            continue
        tree = ET.parse(pom)
        root = tree.getroot()
        dependency_management = root.find("m:dependencyManagement", namespace)
        managed = set(dependency_management.findall(".//m:dependency", namespace)) if dependency_management is not None else set()
        for dependency in root.findall(".//m:dependency", namespace):
            if dependency in managed:
                continue
            group = dependency.findtext("m:groupId", default="", namespaces=namespace)
            artifact = dependency.findtext("m:artifactId", default="", namespaces=namespace)
            version = dependency.findtext("m:version", default="", namespaces=namespace)
            identifier = f"maven:{group}:{artifact}:{version}"
            if identifier not in locked_maven:
                errors.append(f"{pom.relative_to(ROOT)}: direct dependency lacks verified lock row: {identifier}")
        build = root.find("m:build", namespace)
        if build is not None:
            direct_plugins = build.find("m:plugins", namespace)
            if direct_plugins is not None and direct_plugins.findall("m:plugin", namespace):
                errors.append(f"{pom.relative_to(ROOT)}: executable Maven plugins are prohibited in the empty M1 reactor")
    return errors


def validate_repository_binaries() -> list[str]:
    errors: list[str] = []
    prohibited = {".jar", ".class", ".war", ".ear", ".dll", ".so", ".dylib"}
    for path in ROOT.rglob("*"):
        if not path.is_file() or any(part in EXCLUDED_PARTS for part in path.parts):
            continue
        if path.suffix.lower() in prohibited:
            errors.append(f"unapproved binary in repository: {path.relative_to(ROOT)}")
    return errors


def validate_generated(lock: dict[str, object]) -> list[str]:
    errors: list[str] = []
    expected_sbom = canonical_json(make_sbom(lock))
    if not SBOM.is_file() or SBOM.read_text(encoding="utf-8") != expected_sbom:
        errors.append("build/sbom.cdx.json is stale")
    expected_notices = make_notices(lock)
    if not BUILD_NOTICES.is_file() or BUILD_NOTICES.read_text(encoding="utf-8") != expected_notices:
        errors.append("build/THIRD_PARTY_BUILD_NOTICES.md is stale")
    return errors


def command_generate(cache: Path) -> int:
    acquisition = json.loads(ACQUISITION.read_text(encoding="utf-8"))
    verified = [acquire(artifact, cache) for artifact in acquisition["artifacts"]]
    lock = make_lock(acquisition, verified)
    LOCK.write_text(canonical_json(lock), encoding="utf-8")
    SBOM.write_text(canonical_json(make_sbom(lock)), encoding="utf-8")
    BUILD_NOTICES.write_text(make_notices(lock), encoding="utf-8")
    print(f"Generated lock/SBOM/notices for {len(verified)} build and CI artifacts.")
    return 0


def command_validate() -> int:
    acquisition = json.loads(ACQUISITION.read_text(encoding="utf-8"))
    lock = json.loads(LOCK.read_text(encoding="utf-8"))
    errors = validate_schema(acquisition, lock) + validate_poms(lock) + validate_repository_binaries() + validate_generated(lock)
    if errors:
        for error in errors:
            print(f"ERROR: {error}", file=sys.stderr)
        return 1
    print(f"Dependency/licence gate PASS: {len(lock['artifacts'])} locked build/CI artifacts; no product binaries.")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers(dest="command", required=True)
    generate_parser = subparsers.add_parser("generate")
    generate_parser.add_argument("--cache", type=Path, default=ROOT / ".tools" / "downloads")
    subparsers.add_parser("validate")
    arguments = parser.parse_args()
    if arguments.command == "generate":
        return command_generate(arguments.cache)
    return command_validate()


if __name__ == "__main__":
    raise SystemExit(main())
