#!/usr/bin/env python3
"""Validate M24 qualification evidence without manufacturing external results."""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[2]
MANIFEST = ROOT / "build/m24-qualification.json"
EXTERNAL_GATES = ("M22_RUNTIME_CERTIFICATION", "PROVIDER_PROVENANCE",
                  "CERTIFIED_TOOLCHAINS", "BENCHMARK_ENVIRONMENT", "SECURITY_EVIDENCE")
PROFILES = ("SMALL", "SHARED_40", "PROXY_4")
DOCS = ("docs/THREAT_MODEL_M24.md", "docs/PERFORMANCE_QUALIFICATION_M24.md",
        "docs/RECOVERY_QUALIFICATION_M24.md", "docs/RELEASE_QUALIFICATION_M24.md")
VENDOR_PREFIXES = ("ac.grim.", "com.alessiodp.", "com.fastasyncworldedit.", "com.sk89q.",
                   "eu.cloudnetservice.", "eu.decentsoftware.", "lol.pyr.",
                   "me.frep.vulcan.", "net.citizensnpcs.", "net.luckperms.",
                   "net.milkbowl.", "org.mvplugins.")


def read_json(path):
    path = path if isinstance(path, Path) else ROOT / path
    return json.loads(path.read_text(encoding="utf-8"))


def canonical_json(value):
    return json.dumps(value, indent=2, sort_keys=True, ensure_ascii=False) + "\n"


def digest(path):
    value = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            value.update(block)
    return value.hexdigest()


def canonical_permission(value):
    return bool(re.fullmatch(r"zartrabedwars(?:\.[a-z0-9]+)+", value))


def validate_manifest():
    errors = []
    value = read_json(MANIFEST)
    if value.get("schema_version") != 1:
        errors.append("M24 qualification schema must be version 1")
    if value.get("overall_status") != "LOCAL_QUALIFICATION_COMPLETE_EXTERNAL_GATES_PENDING":
        errors.append("M24 overall status must retain external gates")
    if value.get("release_ready") is not False or value.get("support_claim_allowed") is not False:
        errors.append("M24 must not claim release readiness or compatibility support")
    gates = value.get("external_gates", [])
    if tuple(row.get("id") for row in gates) != EXTERNAL_GATES:
        errors.append("M24 external gate inventory/order drift")
    elif any(row.get("status") not in {"PENDING", "PENDING_EXTERNAL"} for row in gates):
        errors.append("M24 external gate advanced without evidence")
    profiles = value.get("performance", {}).get("profiles", [])
    if tuple(row.get("id") for row in profiles) != PROFILES:
        errors.append("M24 benchmark profile inventory/order drift")
    elif any(row.get("status") != "PENDING_NORMATIVE_ENVIRONMENT" or "observed" in row
             for row in profiles):
        errors.append("M24 benchmark results were claimed without the normative environment")
    for relative in DOCS:
        if not (ROOT / relative).is_file():
            errors.append("missing M24 document: " + relative)
    return errors


def validate_claims():
    errors = []
    state = read_json("build/milestone-state.json")
    if (state.get("active_milestone"), state.get("next_milestone")) != ("M22", "M23"):
        errors.append("ordered milestone state must remain on uncertified M22")
    governance = state.get("qualification_governance", {})
    m22, m23, m24 = (governance.get(name, {}) for name in ("M22", "M23", "M24"))
    if (m22.get("implementation_status"), m22.get("certification_status"),
            m22.get("support_claim_allowed")) != ("COMPLETE", "PENDING_EXTERNAL", False):
        errors.append("M22 implementation/certification distinction drift")
    if m23.get("implementation_status") != "CLOSED":
        errors.append("M23 implementation closure missing")
    if (m24.get("status"), m24.get("activation_status")) != (
            "QUALIFICATION_IMPLEMENTED_EXTERNAL_GATES_PENDING", "BLOCKED"):
        errors.append("M24 local qualification status drift")
    if tuple(m24.get("activation_prerequisites", [])) != EXTERNAL_GATES:
        errors.append("M24 activation prerequisite drift")
    matrix = read_json("build/m22-compatibility-matrix.json")
    if matrix.get("policy", {}).get("support_claim_allowed") is not False:
        errors.append("M22 compatibility matrix permits a support claim")
    rows = matrix.get("server_families", []) + matrix.get("client_paths", [])
    if any("PENDING" not in str(row.get("status", "")) for row in rows):
        errors.append("M22 runtime/client row advanced without external evidence")
    lock = read_json("build/m22-provider-lock-requirements.json")
    if (lock.get("policy", {}).get("resolution_allowed") is not False or
            lock.get("policy", {}).get("maven_dependency_declaration_allowed") is not False):
        errors.append("unlocked M22 providers may not be resolved")
    for provider in lock.get("providers", []):
        if (provider.get("artifact_sha256") is not None or
                provider.get("license_text_sha256") is not None or
                provider.get("lock_state") != "REQUIRED_BEFORE_RESOLUTION"):
            errors.append(str(provider.get("id")) + ": unsupported provenance claim")
    return errors


def validate_permissions():
    errors, prior = [], set()
    for number in range(9, 15):
        path = ROOT / f"build/m{number:02d}-permission-inventory.json"
        inventory = read_json(path)
        permissions = [str(item) for item in inventory.get("permissions", [])]
        if inventory.get("count") != len(permissions) or len(set(permissions)) != len(permissions):
            errors.append(path.name + ": permission count/uniqueness drift")
        if any(not canonical_permission(item) for item in permissions):
            errors.append(path.name + ": non-canonical permission")
        if prior and not prior.issubset(set(permissions)):
            errors.append(path.name + ": earlier permission removed")
        prior = set(permissions)
        commands = read_json(ROOT / f"build/m{number:02d}-command-inventory.json").get("commands", [])
        if any(not canonical_permission(str(row.get("permission", ""))) for row in commands):
            errors.append(path.name + ": command permission is non-canonical")
    plugin = (ROOT / "platform/paper/zbw-paper-modern/src/main/resources/plugin.yml").read_text(encoding="utf-8")
    parts = plugin.split("\npermissions:\n", 1)
    if len(parts) != 2:
        return errors + ["Paper plugin.yml permission section missing"]
    declared, current = {}, None
    for line in parts[1].splitlines():
        match = re.fullmatch(r"  ([a-z0-9.]+):", line)
        if match:
            current = match.group(1)
            declared[current] = ""
        default = re.fullmatch(r"    default: (true|false|op|not op)", line)
        if current and default:
            declared[current] = default.group(1)
    if any(not canonical_permission(node) for node in declared):
        errors.append("Paper plugin.yml contains a non-canonical permission")
    privileged = (".admin.", ".staff.", ".manage", ".rollback")
    if any((any(part in node for part in privileged) or node.endswith(".immune")) and default != "op"
           for node, default in declared.items()):
        errors.append("Paper privileged permission does not default to op")
    return errors


def validate_provider_isolation():
    errors = []
    for root in (ROOT / "integrations", ROOT / "cloud"):
        for source in root.rglob("src/main/java/**/*.java"):
            for line in source.read_text(encoding="utf-8").splitlines():
                if line.startswith("import "):
                    imported = line[7:].removesuffix(";").strip()
                    if imported.startswith(VENDOR_PREFIXES):
                        errors.append(f"{source.relative_to(ROOT)}: vendor import {imported}")
    plugin = (ROOT / "platform/paper/zbw-paper-modern/src/main/resources/plugin.yml").read_text(encoding="utf-8")
    required = {"PlaceholderAPI", "Vault", "LuckPerms", "Citizens", "ZNPCsPlus",
                "DecentHolograms", "Parties", "GrimAC", "Vulcan", "WorldEdit",
                "FastAsyncWorldEdit", "WorldGuard", "SlimeWorldManager", "Multiverse-Core"}
    match = re.search(r"^softdepend: \[(.*)]$", plugin, re.MULTILINE)
    present = set() if match is None else {item.strip() for item in match.group(1).split(",")}
    if required - present:
        errors.append("Paper optional providers missing: " + str(sorted(required - present)))
    return errors


def validate_blocking_io():
    errors = []
    paper = ROOT / "platform/paper"
    allowed_files = {"PaperNativeWorldProvider.java", "PrimaryRuntimeCertification.java"}
    for source in sorted(paper.rglob("src/main/java/**/*.java")):
        text = source.read_text(encoding="utf-8")
        relative = source.relative_to(ROOT)
        for marker in ("import java.sql.", "import java.net.http.", "import io.lettuce.", "Thread.sleep("):
            if marker in text:
                errors.append(f"{relative}: blocking primitive {marker}")
        if ("import java.nio.file.Files;" in text or re.search(r"\bFiles\.", text)) and source.name not in allowed_files:
            errors.append(f"{relative}: unaudited filesystem I/O")
        if ".toCompletableFuture().join()" in text and source.name != "PaperFoundationRuntime.java":
            errors.append(f"{relative}: unaudited CompletionStage.join")
        if ".toCompletableFuture().get(" in text and source.name != "PrimaryRuntimeCertification.java":
            errors.append(f"{relative}: unaudited CompletionStage.get")
    world = (paper / "zbw-paper-modern/src/main/java/io/zartra/bedwars/paper/world/PaperNativeWorldProvider.java").read_text(encoding="utf-8")
    if world.count("Affinity.WORKER") < 4 or world.count("thread(false)") < 4:
        errors.append("Paper filesystem steps lost worker-thread guards")
    certification = (paper / "zbw-paper-modern/src/main/java/io/zartra/bedwars/paper/bootstrap/PrimaryRuntimeCertification.java").read_text(encoding="utf-8")
    if "new Thread(() ->" not in certification or "Bukkit.isPrimaryThread()" not in certification:
        errors.append("Paper certification I/O is not worker-isolated")
    adapter = (ROOT / "compatibility/zbw-compat-v1_20-v1_21/src/main/java/io/zartra/bedwars/compat/modern/Paper121CompatibilityAdapter.java").read_text(encoding="utf-8")
    if adapter.count("return CompletableFuture.completedFuture(Result.success(lifecycle));") != 3:
        errors.append("Paper bootstrap join is no longer proven immediately completed")
    return errors


def validate_recovery():
    required = ("storage/zbw-storage-sql/src/test/java/io/zartra/bedwars/storage/sql/SqlFoundationTest.java",
                "storage/zbw-storage-sql/src/test/java/io/zartra/bedwars/storage/sql/ExternalSqlStorageContractTest.java",
                "redis/zbw-redis/src/test/java/io/zartra/bedwars/redis/RedisFailureRecoveryTest.java",
                "proxy/zbw-proxy-api/src/test/java/io/zartra/bedwars/proxy/api/ProxyFailureSecurityPerformanceTest.java",
                "replay/zbw-replay-sql/src/test/java/io/zartra/bedwars/replay/sql/JdbcReplayRepositoryTest.java",
                "atlas/zbw-atlas-sql/src/test/java/io/zartra/bedwars/atlas/sql/JdbcAtlasRepositoryTest.java",
                "application/zbw-application/src/test/java/io/zartra/bedwars/application/migration/M23MigrationEngineTest.java",
                "docs/BACKUP_RESTORE_M04.md", "docs/OPERATIONAL_DEFAULTS.md")
    return ["missing recovery evidence: " + path for path in required if not (ROOT / path).is_file()]


def validate_release():
    errors = []
    pom = (ROOT / "pom.xml").read_text(encoding="utf-8")
    timestamp = "<project.build.outputTimestamp>2026-07-14T00:00:00Z</project.build.outputTimestamp>"
    if timestamp not in pom:
        errors.append("root Maven build lacks immutable output timestamp")
    if len(read_json("build/maven-dependency-lock.json").get("components", [])) != len(read_json("build/maven-build-sbom.cdx.json").get("components", [])):
        errors.append("Maven lock/SBOM component count mismatch")
    if len(read_json("build/dependency-lock.json").get("artifacts", [])) != len(read_json("build/sbom.cdx.json").get("components", [])):
        errors.append("build lock/SBOM component count mismatch")
    required = ("THIRD_PARTY_NOTICES.md", "build/THIRD_PARTY_BUILD_NOTICES.md",
                "build/M04_MAVEN_BUILD_NOTICES.md", "docs/RUNTIME_COMPATIBILITY_MATRIX.md")
    errors.extend("missing release input: " + path for path in required if not (ROOT / path).is_file())
    excluded, prohibited = {".git", ".m2", ".tools", "target"}, {".class", ".dll", ".dylib", ".exe", ".jar", ".so"}
    for path in ROOT.rglob("*"):
        if path.is_file() and path.suffix.lower() in prohibited and not any(part in excluded for part in path.parts):
            errors.append("prohibited repository binary: " + str(path.relative_to(ROOT)))
    return errors


def validate_all():
    errors = []
    for validator in (validate_manifest, validate_claims, validate_permissions,
                      validate_provider_isolation, validate_blocking_io,
                      validate_recovery, validate_release):
        errors.extend(validator())
    return errors


def artifact_inventory():
    graph = read_json("build/module-graph.json")
    artifacts = []
    for module in graph.get("materialized_build_modules", []):
        if module.get("packaging") != "jar":
            continue
        target = (ROOT / str(module["path"])).parent / "target"
        candidates = sorted(path for path in target.glob("*.jar")
                            if not any(tag in path.name for tag in (
                                "-javadoc", "-sources", "-tests",
                                "-shaded", "original-")))
        for path in candidates:
            artifacts.append({"module": module["id"], "path": path.relative_to(ROOT).as_posix(),
                              "sha256": digest(path), "size": path.stat().st_size})
    return {"schema_version": 1, "artifacts": artifacts}


def compare_artifact_reports(first, second):
    if first.get("schema_version") != 1 or second.get("schema_version") != 1:
        return ["artifact reports must use schema version 1"]
    return [] if first.get("artifacts") == second.get("artifacts") else ["artifact checksum reports differ"]


def main():
    parser = argparse.ArgumentParser()
    commands = parser.add_subparsers(dest="command")
    commands.add_parser("validate")
    report = commands.add_parser("artifact-report")
    report.add_argument("--output", type=Path, required=True)
    compare = commands.add_parser("compare-artifacts")
    compare.add_argument("--first", type=Path, required=True)
    compare.add_argument("--second", type=Path, required=True)
    arguments = parser.parse_args()
    if arguments.command == "artifact-report":
        arguments.output.parent.mkdir(parents=True, exist_ok=True)
        arguments.output.write_text(canonical_json(artifact_inventory()), encoding="utf-8", newline="\n")
        print("M24 artifact report written: " + str(arguments.output))
        return 0
    errors = (compare_artifact_reports(read_json(arguments.first), read_json(arguments.second))
              if arguments.command == "compare-artifacts" else validate_all())
    for error in errors:
        print("ERROR: " + error, file=sys.stderr)
    if errors:
        return 1
    print("M24 qualification PASS: local evidence verified; M22 runtime, provider provenance and normative benchmarks remain pending.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())