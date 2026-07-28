#!/usr/bin/env python3
"""Capture, seed, restore and validate the exact current Maven repository."""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import shutil
import tempfile
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET
import uuid


ROOT = Path(__file__).resolve().parents[2]
LOCK = ROOT / "build" / "maven-dependency-lock.json"
SBOM = ROOT / "build" / "maven-build-sbom.cdx.json"
NOTICES = ROOT / "build" / "M04_MAVEN_BUILD_NOTICES.md"
DEFAULT_STAGING = ROOT / "target" / "m04-staging"
DEFAULT_REPOSITORY = ROOT / ".m2" / "repository"
CENTRAL = "https://repo.maven.apache.org/maven2/"
EXTENDEDCLIP = "https://repo.extendedclip.com/content/repositories/placeholderapi/"
SPDX_COMMIT = "c4a7237ec8f4654e867546f9f409749300f1bf4c"
SPDX_TEXT = f"https://raw.githubusercontent.com/spdx/license-list-data/{SPDX_COMMIT}/text/"
JDOM_LICENSE_SOURCE = (
    "https://raw.githubusercontent.com/hunterhacker/jdom/"
    "JDOM-2.0.6.1/LICENSE.txt")
NAMESPACE = "http://maven.apache.org/POM/4.0.0"
PLEXUS_I18N_SOURCE = (
    "https://raw.githubusercontent.com/codehaus-plexus/plexus-i18n/"
    "d5aaf49970fc3e95408f4f9cd1b856fa72be130f/"
    "src/main/java/org/codehaus/plexus/i18n/I18N.java")
PAPER_LOCK = ROOT / "build" / "m06-paper-runtime-lock.json"
PAPER_PREFIX = "io/zartra/mirror/paper/paper-api/1.21.1-build133/"
PAPER_GENERATED_POM = "generated:tools/dependencies/acquire_m06_paper.py"


def digest(path: Path) -> str:
    value = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            value.update(block)
    return value.hexdigest()


def canonical_json(value: object) -> str:
    return json.dumps(value, indent=2, sort_keys=True, ensure_ascii=False) + "\n"


def coordinate(relative: Path) -> tuple[str, str, str]:
    parts = relative.parts
    if len(parts) < 4:
        raise ValueError(f"Invalid Maven repository path: {relative}")
    return ".".join(parts[:-3]), parts[-3], parts[-2]


def normalize_license(name: str, url: str = "") -> tuple[str, list[str]]:
    lowered = name.strip().lower()
    evidence_text = lowered + " " + url.strip().lower()
    if "apache" in lowered and ("2.0" in lowered or "version 2" in lowered):
        return "Apache-2.0", ["Apache-2.0"]
    if "eclipse public license" in lowered or lowered == "epl-2.0":
        return "EPL-2.0", ["EPL-2.0"]
    if lowered == "mit-0":
        return "MIT-0", ["MIT-0"]
    if lowered in {"mit", "mit license", "the mit license"}:
        return "MIT", ["MIT"]
    if "mozilla public license version 2" in lowered:
        return "MPL-2.0", ["MPL-2.0"]
    if lowered == "mpl 1.1":
        return "MPL-1.1", ["MPL-1.1"]
    if "lesser general public license" in lowered or lowered.startswith("lgpl"):
        if "3.0" in evidence_text:
            return "LGPL-3.0-only", ["LGPL-3.0-only"]
        return "LGPL-2.1-or-later", ["LGPL-2.1-or-later"]
    if "general public license v3.0" in lowered or lowered in {"gpl-3.0", "gpl-3.0-only"}:
        return "GPL-3.0-only", ["GPL-3.0-only"]
    if lowered in {"bsd-3-clause", "the bsd license", "new bsd license", "modified bsd 3-clause license"}:
        return "BSD-3-Clause", ["BSD-3-Clause"]
    if "bsd" in lowered and ("2-clause" in lowered or "2.0" in lowered):
        return "BSD-2-Clause", ["BSD-2-Clause"]
    if lowered == "wtfpl":
        return "WTFPL", ["WTFPL"]
    if lowered == "public domain":
        return "LicenseRef-Public-Domain", []
    if lowered == "plexus":
        return "LicenseRef-Plexus", []
    if lowered == "similar to apache license but with the acknowledgment clause removed":
        return "LicenseRef-JDOM", []
    if "cddl/gplv2" in lowered:
        return "CDDL-1.1 OR GPL-2.0-only WITH Classpath-exception-2.0", [
            "CDDL-1.1", "GPL-2.0-only", "Classpath-exception-2.0"]
    if lowered == "cddl 1.1":
        return "CDDL-1.1", ["CDDL-1.1"]
    if "gpl2 w/ cpe" in lowered or "general public license, version 2 with" in lowered:
        return "GPL-2.0-only WITH Classpath-exception-2.0", [
            "GPL-2.0-only", "Classpath-exception-2.0"]
    if "universal foss exception" in lowered:
        return "GPL-2.0-only WITH LicenseRef-Universal-FOSS-exception-1.0", [
            "GPL-2.0-only"]
    raise ValueError(f"Unrecognized Maven licence declaration: {name!r}")


def fetch_bytes(url: str) -> bytes:
    request = urllib.request.Request(url, headers={"User-Agent": "ZartraBedWars-M04/1"})
    with urllib.request.urlopen(request, timeout=90) as response:
        return response.read()


def license_evidence(spdx_ids: list[str]) -> list[dict[str, str]]:
    evidence = []
    for identifier in spdx_ids:
        url = SPDX_TEXT + urllib.parse.quote(identifier, safe="") + ".txt"
        content = fetch_bytes(url)
        evidence.append({"spdx": identifier, "source": url,
                         "sha256": hashlib.sha256(content).hexdigest()})
    return evidence


def license_override(identifier: str) -> list[dict[str, object]]:
    if identifier == "maven:me.clip:placeholderapi:2.12.2":
        return [{"declared_name": "GNU General Public License v3.0",
                 "declared_url": "https://www.gnu.org/licenses/gpl-3.0.txt",
                 "spdx_expression": "GPL-3.0-only", "evidence_ids": ["GPL-3.0-only"]}]
    if identifier != "maven:org.codehaus.plexus:plexus-i18n:1.0-beta-10":
        return []
    source = fetch_bytes(PLEXUS_I18N_SOURCE)
    if b"Licensed under the Apache License, Version 2.0" not in source:
        raise ValueError("plexus-i18n immutable source licence header changed")
    return [{
        "declared_name": "Apache-2.0 source header at immutable release tag",
        "declared_url": PLEXUS_I18N_SOURCE,
        "spdx_expression": "Apache-2.0",
        "evidence_ids": ["Apache-2.0"],
        "source_header_sha256": hashlib.sha256(source).hexdigest(),
    }]


def element_name(root: ET.Element, name: str) -> str:
    return f"{{{root.tag[1:].split('}', 1)[0]}}}{name}" if root.tag.startswith("{") else name


def parent_pom(path: Path, repository: Path, root: ET.Element) -> Path | None:
    parent = root.find(element_name(root, "parent"))
    if parent is None:
        return None
    group = (parent.findtext(element_name(root, "groupId")) or "").strip()
    artifact = (parent.findtext(element_name(root, "artifactId")) or "").strip()
    version = (parent.findtext(element_name(root, "version")) or "").strip()
    if not group or not artifact or not version or "${" in version:
        raise ValueError(f"Cannot resolve licence parent for {path}")
    relative = Path(*group.split(".")) / artifact / version / f"{artifact}-{version}.pom"
    candidate = repository / relative
    if not candidate.is_file():
        candidate.parent.mkdir(parents=True, exist_ok=True)
        candidate.write_bytes(fetch_bytes(CENTRAL + relative.as_posix()))
    return candidate


def pom_licenses(path: Path, repository: Path, seen: set[Path] | None = None) -> list[dict[str, object]]:
    visited = set() if seen is None else set(seen)
    resolved = path.resolve()
    if resolved in visited:
        raise ValueError(f"Cyclic Maven parent while resolving licences: {path}")
    visited.add(resolved)
    root = ET.parse(path).getroot()
    licenses = []
    q = lambda name: element_name(root, name)
    for node in root.findall(f"{q('licenses')}/{q('license')}"):
        name = (node.findtext(q("name")) or "").strip()
        url = (node.findtext(q("url")) or "").strip()
        expression, identifiers = normalize_license(name, url)
        licenses.append({"declared_name": name, "declared_url": url,
                         "spdx_expression": expression, "evidence_ids": identifiers})
    if licenses:
        return licenses
    parent = parent_pom(path, repository, root)
    return [] if parent is None else pom_licenses(parent, repository, visited)


def matching_pom(jar: Path, repository: Path) -> Path:
    candidates = sorted(jar.parent.glob("*.pom"))
    if not candidates:
        relative = jar.relative_to(repository)
        group, artifact, version = coordinate(relative)
        candidate = jar.parent / f"{artifact}-{version}.pom"
        candidate.write_bytes(fetch_bytes(
            CENTRAL + "/".join(group.split(".")) + f"/{artifact}/{version}/{candidate.name}"))
        candidates = [candidate]
    if len(candidates) != 1:
        raise ValueError(f"Expected one POM beside {jar}")
    return candidates[0]


def make_sbom(lock: dict[str, object]) -> dict[str, object]:
    lock_digest = hashlib.sha256(canonical_json(lock).encode("utf-8")).hexdigest()
    components = []
    for component in lock["components"]:
        components.append({
            "type": "library",
            "bom-ref": component["id"],
            "group": component["group"],
            "name": component["artifact"],
            "version": component["version"],
            "licenses": [
                {"expression": record["spdx_expression"]}
                for record in component["licenses"]
            ],
            "properties": [
                {"name": "zartra.scope", "value": component["scope"]},
                {"name": "zartra.productBundled", "value": "false"},
                {"name": "zartra.commercialUse", "value": "true"},
            ],
        })
    return {
        "bomFormat": "CycloneDX",
        "specVersion": "1.6",
        "serialNumber": f"urn:uuid:{uuid.UUID(lock_digest[:32])}",
        "version": 1,
        "metadata": {"component": {
            "type": "application", "name": "ZartraBedWars M06 build/runtime/test graph",
            "version": "0.6.0-SNAPSHOT"}},
        "components": components,
    }


def make_notices(lock: dict[str, object]) -> str:
    lines = [
        "# Milestone 6 Maven Dependency Notices",
        "",
        "These components are resolved for the M06 thin-artifact build, runtime adapters or tests. No dependency is bundled in a ZartraBedWars product artifact.",
        "",
        "| Coordinates | Licence declaration | Scope | Product bundled |",
        "|---|---|---|---|",
    ]
    for component in lock["components"]:
        licenses = "; ".join(record["spdx_expression"] for record in component["licenses"])
        lines.append(
            f"| `{component['group']}:{component['artifact']}:{component['version']}` | "
            f"{licenses} | {component['scope']} | NO |")
    lines.extend([
        "",
        "Generated deterministically from `build/maven-dependency-lock.json`. Product redistribution, shading and modification: none.",
        "",
    ])
    return "\n".join(lines)


def capture(repository: Path) -> int:
    for jar in sorted(repository.rglob("*.jar")):
        pom_licenses(matching_pom(jar, repository), repository)
    files = sorted(
        path for path in repository.rglob("*")
        if path.is_file()
        and path.suffix in {".jar", ".pom"}
        and not path.relative_to(repository).as_posix().startswith("io/zartra/bedwars/"))
    artifacts = []
    evidence_cache: dict[str, list[dict[str, str]]] = {}
    components: dict[str, dict[str, object]] = {}
    missing_licenses: list[str] = []
    for path in files:
        relative = path.relative_to(repository)
        group, artifact, version = coordinate(relative)
        identifier = f"maven:{group}:{artifact}:{version}"
        artifacts.append({"relative_path": relative.as_posix(), "sha256": digest(path),
                          "size": path.stat().st_size, "source": CENTRAL + relative.as_posix()})
        if path.suffix != ".jar":
            continue
        declarations = pom_licenses(matching_pom(path, repository), repository)
        if not declarations:
            declarations = license_override(identifier)
        if not declarations:
            missing_licenses.append(identifier)
            continue
        for declaration in declarations:
            expression = str(declaration["spdx_expression"])
            if expression not in evidence_cache:
                evidence_cache[expression] = license_evidence(list(declaration.pop("evidence_ids")))
            declaration["text_evidence"] = evidence_cache[expression]
            if expression == "LicenseRef-Public-Domain":
                declaration["declaration_sha256"] = hashlib.sha256(b"Public Domain").hexdigest()
            if expression == "LicenseRef-Plexus":
                declaration["declaration_sha256"] = hashlib.sha256(
                    (str(declaration["declared_name"]) + "\n" + str(declaration["declared_url"])).encode("utf-8")).hexdigest()
            if expression == "LicenseRef-JDOM":
                source = fetch_bytes(JDOM_LICENSE_SOURCE)
                if b"Copyright (C) 2000-2012 Jason Hunter & Brett McLaughlin" not in source:
                    raise ValueError("JDOM immutable release licence text changed")
                declaration["declared_url"] = JDOM_LICENSE_SOURCE
                declaration["declaration_sha256"] = hashlib.sha256(source).hexdigest()
        component = components.setdefault(identifier, {
            "id": identifier, "group": group, "artifact": artifact, "version": version,
            "scope": "RUNTIME_OR_BUILD_NOT_BUNDLED", "product_bundled": False,
            "redistribution": False, "shading": False, "modification": False,
            "commercial_use": True, "licenses": declarations, "files": []})
        component["files"].append(relative.as_posix())
    if missing_licenses:
        raise ValueError("Binaries without inherited licence declaration:\n" +
                         "\n".join(sorted(missing_licenses)))
    lock = {"schema_version": 1, "baseline_date": "2026-07-14",
            "repository": CENTRAL, "spdx_license_list_commit": SPDX_COMMIT,
            "policy": {"dynamic_versions": False, "product_bundled": False,
                       "offline_build_after_seed": True},
            "components": sorted(components.values(), key=lambda item: str(item["id"])),
            "artifacts": artifacts}
    LOCK.write_text(canonical_json(lock), encoding="utf-8")
    SBOM.write_text(canonical_json(make_sbom(lock)), encoding="utf-8")
    NOTICES.write_text(make_notices(lock), encoding="utf-8")
    print(f"Captured {len(artifacts)} Maven files and {len(components)} binary components.")
    return 0


def paper_pom_bytes() -> bytes:
    """Return the immutable local-mirror POM generated by the approved acquisition tool."""
    return (
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        "<project xmlns=\"http://maven.apache.org/POM/4.0.0\">\n"
        "  <modelVersion>4.0.0</modelVersion>\n"
        "  <groupId>io.zartra.mirror.paper</groupId>\n"
        "  <artifactId>paper-api</artifactId>\n"
        "  <version>1.21.1-build133</version>\n"
        "  <name>Paper API build 133 immutable compile-only mirror</name>\n"
        "  <licenses><license><name>GNU General Public License v3.0 only</name>"
        "<url>https://www.gnu.org/licenses/gpl-3.0-standalone.html</url>"
        "<distribution>repo</distribution></license></licenses>\n"
        "</project>\n"
    ).encode("utf-8")


def include_paper(repository: Path) -> int:
    """Add only the exact non-redistributed Paper API mirror to the existing approved lock."""
    lock = json.loads(LOCK.read_text(encoding="utf-8"))
    runtime = json.loads(PAPER_LOCK.read_text(encoding="utf-8"))["paper"]["api"]
    version = "1.21.1-build133"
    directory = repository / Path(PAPER_PREFIX)
    jar = directory / f"paper-api-{version}.jar"
    pom = directory / f"paper-api-{version}.pom"
    if not jar.is_file() or hashlib.sha512(jar.read_bytes()).hexdigest() != runtime["sha512"]:
        raise ValueError("Paper API mirror is absent or differs from the pre-resolution SHA-512")
    if not pom.is_file() or pom.read_bytes() != paper_pom_bytes():
        raise ValueError("Paper API generated POM differs from the approved deterministic bytes")
    identifier = "maven:io.zartra.mirror.paper:paper-api:1.21.1-build133"
    lock["artifacts"] = [row for row in lock["artifacts"]
                         if not row["relative_path"].startswith(PAPER_PREFIX)]
    lock["components"] = [row for row in lock["components"] if row["id"] != identifier]
    jar_relative = jar.relative_to(repository).as_posix()
    pom_relative = pom.relative_to(repository).as_posix()
    lock["artifacts"].extend([
        {"relative_path": jar_relative, "sha256": digest(jar), "size": jar.stat().st_size,
         "source": runtime["url"]},
        {"relative_path": pom_relative, "sha256": digest(pom), "size": pom.stat().st_size,
         "source": PAPER_GENERATED_POM},
    ])
    evidence = license_evidence(["GPL-3.0-only"])
    lock["components"].append({
        "id": identifier, "group": "io.zartra.mirror.paper", "artifact": "paper-api",
        "version": version, "scope": "COMPILE_ONLY_PLATFORM_PROVIDED_NOT_BUNDLED",
        "product_bundled": False, "redistribution": False, "shading": False,
        "modification": False, "commercial_use": True,
        "licenses": [{"declared_name": "GNU General Public License v3.0 only",
                      "declared_url": "https://www.gnu.org/licenses/gpl-3.0-standalone.html",
                      "spdx_expression": "GPL-3.0-only", "text_evidence": evidence}],
        "files": [jar_relative, pom_relative],
    })
    lock["artifacts"] = sorted(lock["artifacts"], key=lambda row: row["relative_path"])
    lock["components"] = sorted(lock["components"], key=lambda row: row["id"])
    lock["baseline_date"] = "2026-07-15"
    LOCK.write_text(canonical_json(lock), encoding="utf-8")
    SBOM.write_text(canonical_json(make_sbom(lock)), encoding="utf-8")
    NOTICES.write_text(make_notices(lock), encoding="utf-8")
    print("Paper API lock PASS: exact build 133 API, GPL-3.0-only, product bundled: no.")
    return 0


def validate_lock() -> list[str]:
    errors = []
    lock = json.loads(LOCK.read_text(encoding="utf-8"))
    paths = set()
    for artifact in lock["artifacts"]:
        relative = artifact["relative_path"]
        if relative in paths:
            errors.append(f"duplicate Maven path: {relative}")
        paths.add(relative)
        if not (artifact["source"].startswith(CENTRAL)
                or artifact["source"].startswith(EXTENDEDCLIP)
                or relative.startswith(PAPER_PREFIX)
                and (artifact["source"].startswith("https://repo.papermc.io/")
                     or artifact["source"] == PAPER_GENERATED_POM)):
            errors.append(f"non-authoritative Maven source: {relative}")
        if len(artifact["sha256"]) != 64 or artifact["size"] <= 0:
            errors.append(f"invalid Maven hash/size: {relative}")
    for component in lock["components"]:
        if not component["licenses"]:
            errors.append(f"missing Maven licence: {component['id']}")
        if any(component[field] for field in ("product_bundled", "redistribution", "shading", "modification")):
            errors.append(f"Maven thin-artifact component has product rights enabled: {component['id']}")
        for license_record in component["licenses"]:
            if not license_record["spdx_expression"]:
                errors.append(f"empty SPDX expression: {component['id']}")
            if not license_record["text_evidence"] and "declaration_sha256" not in license_record:
                errors.append(f"missing licence evidence: {component['id']}")
    if not SBOM.is_file() or SBOM.read_text(encoding="utf-8") != canonical_json(make_sbom(lock)):
        errors.append("build/maven-build-sbom.cdx.json is stale")
    if not NOTICES.is_file() or NOTICES.read_text(encoding="utf-8") != make_notices(lock):
        errors.append("build/M04_MAVEN_BUILD_NOTICES.md is stale")
    return errors


def validate(repository: Path, require_files: bool) -> int:
    errors = validate_lock()
    lock = json.loads(LOCK.read_text(encoding="utf-8"))
    if require_files:
        for artifact in lock["artifacts"]:
            path = repository / artifact["relative_path"]
            if not path.is_file() or digest(path) != artifact["sha256"]:
                errors.append(f"missing or changed Maven artifact: {artifact['relative_path']}")
    if errors:
        for error in errors:
            print(f"ERROR: {error}")
        return 1
    print(f"Maven dependency/licence lock PASS: {len(lock['components'])} components, "
          f"{len(lock['artifacts'])} exact files; product bundled: zero.")
    return 0


def seed(source: Path, destination: Path) -> int:
    if validate(source, True):
        return 1
    lock = json.loads(LOCK.read_text(encoding="utf-8"))
    for artifact in lock["artifacts"]:
        source_path = source / artifact["relative_path"]
        destination_path = destination / artifact["relative_path"]
        destination_path.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source_path, destination_path)
    return validate(destination, True)


def restore(destination: Path) -> int:
    errors = validate_lock()
    if errors:
        for error in errors:
            print(f"ERROR: {error}")
        return 1
    lock = json.loads(LOCK.read_text(encoding="utf-8"))
    for artifact in lock["artifacts"]:
        destination_path = destination / artifact["relative_path"]
        if destination_path.is_file() and digest(destination_path) == artifact["sha256"]:
            continue
        destination_path.parent.mkdir(parents=True, exist_ok=True)
        content = (paper_pom_bytes() if artifact["source"] == PAPER_GENERATED_POM
                   else fetch_bytes(artifact["source"]))
        if hashlib.sha256(content).hexdigest() != artifact["sha256"]:
            raise ValueError(f"Downloaded Maven checksum mismatch: {artifact['relative_path']}")
        with tempfile.NamedTemporaryFile(dir=destination_path.parent, delete=False) as temporary:
            temporary.write(content)
            temporary_path = Path(temporary.name)
        temporary_path.replace(destination_path)
    return validate(destination, True)


def main() -> int:
    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers(dest="command", required=True)
    capture_parser = subparsers.add_parser("capture")
    capture_parser.add_argument("--repository", type=Path, default=DEFAULT_STAGING)
    validate_parser = subparsers.add_parser("validate")
    validate_parser.add_argument("--repository", type=Path, default=DEFAULT_REPOSITORY)
    validate_parser.add_argument("--require-files", action="store_true")
    seed_parser = subparsers.add_parser("seed")
    seed_parser.add_argument("--source", type=Path, default=DEFAULT_STAGING)
    seed_parser.add_argument("--destination", type=Path, default=DEFAULT_REPOSITORY)
    restore_parser = subparsers.add_parser("restore")
    restore_parser.add_argument("--destination", type=Path, default=DEFAULT_REPOSITORY)
    paper_parser = subparsers.add_parser("include-paper")
    paper_parser.add_argument("--repository", type=Path, default=DEFAULT_REPOSITORY)
    arguments = parser.parse_args()
    if arguments.command == "capture":
        return capture(arguments.repository)
    if arguments.command == "validate":
        return validate(arguments.repository, arguments.require_files)
    if arguments.command == "seed":
        return seed(arguments.source, arguments.destination)
    if arguments.command == "include-paper":
        return include_paper(arguments.repository)
    return restore(arguments.destination)


if __name__ == "__main__":
    raise SystemExit(main())
