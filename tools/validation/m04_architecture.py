#!/usr/bin/env python3
"""Deterministic architecture and storage-governance checks for Milestone 4."""

from __future__ import annotations

import json
from pathlib import Path
import re
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[2]
API_MODULE = "storage/zbw-storage-api"
SQL_MODULE = "storage/zbw-storage-sql"
MAVEN = {"m": "http://maven.apache.org/POM/4.0.0"}
FORBIDDEN_PLATFORM = (
    "org.bukkit", "io.papermc", "net.minecraft", "com.velocitypowered",
    "net.md_5.bungee", "redis.clients", "io.lettuce",
)
SQL_MARKERS = (
    "java.sql", "javax.sql", "PreparedStatement", "CREATE TABLE", "INSERT INTO",
    "UPDATE zbw_", "DELETE FROM zbw_", "SELECT ",
)


def sources(module: str) -> list[Path]:
    return sorted((ROOT / module / "src" / "main" / "java").rglob("*.java"))


def dependencies(module: str) -> dict[str, str]:
    pom = ET.parse(ROOT / module / "pom.xml").getroot()
    result: dict[str, str] = {}
    for dependency in pom.findall("m:dependencies/m:dependency", MAVEN):
        group = dependency.findtext("m:groupId", default="", namespaces=MAVEN)
        artifact = dependency.findtext("m:artifactId", default="", namespaces=MAVEN)
        scope = dependency.findtext("m:scope", default="compile", namespaces=MAVEN)
        result[f"{group}:{artifact}"] = scope
    return result


def validate() -> list[str]:
    errors: list[str] = []
    state = json.loads((ROOT / "build/milestone-state.json").read_text(encoding="utf-8"))
    if state["active_milestone"] not in {"M04", "M05", "M06", "M07", None}:
        errors.append("active milestone must preserve the M04 baseline")
    if state["completed_milestones"] not in (
            ["M00", "M01", "M02", "M03"],
            ["M00", "M01", "M02", "M03", "M04"],
            ["M00", "M01", "M02", "M03", "M04", "M05"],
            ["M00", "M01", "M02", "M03", "M04", "M05", "M06"],
            ["M00", "M01", "M02", "M03", "M04", "M05", "M06", "M07"]):
        errors.append("completed milestones must be the ordered M04 implementation or closure set")

    for module in (API_MODULE, SQL_MODULE):
        root = ROOT / module / "src/main/java"
        if not root.is_dir():
            errors.append(f"missing M04 source root: {module}")
            continue
        packages = {path.parent for path in sources(module) if path.name != "package-info.java"}
        for package in sorted(packages):
            if not (package / "package-info.java").is_file():
                errors.append(f"M04 package lacks package-info.java: {package.relative_to(ROOT)}")
        for path in sources(module):
            content = path.read_text(encoding="utf-8")
            for forbidden in FORBIDDEN_PLATFORM:
                if forbidden in content:
                    errors.append(f"platform/proxy/Redis dependency in M04: {path.relative_to(ROOT)}")
            if re.search(r"\b(?:TODO|FIXME)\b|UnsupportedOperationException|\bstub\b|fake implementation",
                         content, re.IGNORECASE):
                errors.append(f"incomplete production marker: {path.relative_to(ROOT)}")
            if re.search(r"\bstatic\s+(?!final\b)[^;=()]+(?:=|;)", content):
                errors.append(f"global mutable state in M04: {path.relative_to(ROOT)}")
            for unbounded in ("newCachedThreadPool", "newFixedThreadPool", "newSingleThreadExecutor",
                              "new Thread(", "Executors."):
                if unbounded in content:
                    errors.append(f"scheduler/thread ownership leaked into M04: {path.relative_to(ROOT)}")

    for path in sources(API_MODULE):
        content = path.read_text(encoding="utf-8")
        for marker in SQL_MARKERS + ("com.zaxxer", "com.github.benmanes.caffeine", "org.flywaydb"):
            if marker in content:
                errors.append(f"JDBC/implementation dependency leaked into storage API: {path.relative_to(ROOT)}")

    production_roots = (
        "api/zbw-api", "domain/zbw-domain", "application/zbw-application",
        "configuration/zbw-config", "sdk/zbw-sdk",
        "integrations/discord/zbw-integration-discord-api", API_MODULE,
    )
    for module in production_roots:
        for path in sources(module):
            content = path.read_text(encoding="utf-8")
            if any(marker in content for marker in SQL_MARKERS):
                errors.append(f"SQL exists outside the SQL adapter: {path.relative_to(ROOT)}")

    api_dependencies = dependencies(API_MODULE)
    if api_dependencies != {
            "io.zartra.bedwars:zbw-api": "compile",
            "io.zartra.bedwars:zbw-domain": "compile",
            "org.junit.jupiter:junit-jupiter": "test"}:
        errors.append(f"storage API dependency boundary drifted: {api_dependencies}")
    sql_dependencies = dependencies(SQL_MODULE)
    expected_compile = {
        "io.zartra.bedwars:zbw-storage-api", "com.zaxxer:HikariCP",
        "com.github.ben-manes.caffeine:caffeine",
    }
    actual_compile = {key for key, scope in sql_dependencies.items() if scope == "compile"}
    if actual_compile != expected_compile:
        errors.append(f"SQL adapter compile dependencies drifted: {sorted(actual_compile)}")
    for required in ("org.flywaydb:flyway-core", "org.xerial:sqlite-jdbc",
                     "org.mariadb.jdbc:mariadb-java-client", "com.mysql:mysql-connector-j",
                     "org.testcontainers:mysql", "org.testcontainers:mariadb"):
        if required not in sql_dependencies:
            errors.append(f"missing approved M04 dependency: {required}")

    graph = json.loads((ROOT / "build/module-graph.json").read_text(encoding="utf-8"))
    materialized = {row["id"] for row in graph["materialized_build_modules"]}
    if not {"zbw-storage-api", "zbw-storage-sql"}.issubset(materialized):
        errors.append("storage modules are absent from the materialized graph")
    if state["active_milestone"] == "M04":
        for later in ("observability", "platform", "proxy", "gameplay", "replay", "atlas"):
            if (ROOT / later).exists():
                errors.append(f"post-M04 production path materialized: {later}")

    required_tests = (
        "StoragePrimitiveTest.java", "SQLiteStorageContractTest.java",
        "ExternalSqlStorageContractTest.java", "SqlBranchAndFailureTest.java",
    )
    test_names = {path.name for path in (ROOT / "storage").rglob("*Test.java")}
    for required in required_tests:
        if required not in test_names:
            errors.append(f"missing M04 contract suite: {required}")
    external_test = (ROOT / SQL_MODULE /
                     "src/test/java/io/zartra/bedwars/storage/sql/ExternalSqlStorageContractTest.java")
    if external_test.is_file():
        text = external_test.read_text(encoding="utf-8")
        for token in ("MySQLContainer", "MariaDBContainer", "@sha256:",
                      "ZBW_REQUIRE_EXTERNAL_DATABASES", "ZBW_TEST_DATABASE_IMAGE",
                      "query-plans.json", "pool-health.json", "backup-restore.json"):
            if token not in text:
                errors.append(f"external SQL contract lacks {token}")

    image_lock = ROOT / "build/m04-database-container-lock.json"
    workflow = ROOT / ".github/workflows/m04-external-database-contracts.yml"
    certifier = ROOT / "tools/ci/certify_m04_external.py"
    for required in (image_lock, workflow, certifier):
        if not required.is_file():
            errors.append(f"missing RC-077 verification asset: {required.relative_to(ROOT)}")
    if image_lock.is_file():
        locked_images = json.loads(image_lock.read_text(encoding="utf-8"))
        references = {row["engine"]: row["reference"] for row in locked_images["images"]}
        if set(references) != {"mysql", "mariadb"}:
            errors.append("RC-077 image lock must contain MySQL and MariaDB")
        for engine, reference in references.items():
            if not re.fullmatch(
                    rf"docker\.io/library/{engine}:[0-9]+(?:\.[0-9]+)+@sha256:[0-9a-f]{{64}}",
                    reference):
                errors.append(f"RC-077 {engine} image is not exact tag@digest")
    if workflow.is_file():
        workflow_text = workflow.read_text(encoding="utf-8")
        for token in ("mysql", "mariadb", "ZBW_REQUIRE_EXTERNAL_DATABASES: \"1\"",
                      "certify_m04_external.py", "if-no-files-found: error",
                      "actions/upload-artifact@ea165f8d65b6e75b540449e92b4886f43607fa02"):
            if token not in workflow_text:
                errors.append(f"RC-077 workflow lacks {token}")

    lock = json.loads((ROOT / "build/maven-dependency-lock.json").read_text(encoding="utf-8"))
    locked = {row["id"] for row in lock["components"]}
    for coordinate in (
            "maven:com.zaxxer:HikariCP:4.0.3",
            "maven:com.github.ben-manes.caffeine:caffeine:2.9.3",
            "maven:org.flywaydb:flyway-core:10.20.1",
            "maven:org.xerial:sqlite-jdbc:3.46.0.0",
            "maven:org.mariadb.jdbc:mariadb-java-client:3.4.1",
            "maven:com.mysql:mysql-connector-j:8.4.0",
            "maven:org.testcontainers:testcontainers:1.20.4"):
        if coordinate not in locked:
            errors.append(f"M04 binary absent from exact lock: {coordinate}")
    if any(row["product_bundled"] for row in lock["components"]):
        errors.append("M04 thin-artifact dependency lock enables product bundling")

    required_docs = (
        "docs/IMPLEMENTATION_M04.md", "docs/API_M04.md", "docs/MIGRATIONS_M04.md",
        "docs/BACKUP_RESTORE_M04.md", "docs/QUERY_PLANS_M04.md", "docs/POOL_HEALTH_M04.md",
    )
    for relative in required_docs:
        if not (ROOT / relative).is_file():
            errors.append(f"missing M04 documentation: {relative}")
    return errors


def main() -> int:
    errors = validate()
    if errors:
        for error in errors:
            print(f"ERROR: {error}")
        return 1
    print("M04 architecture PASS: JDBC-free API, confined SQL, bounded pools/cache, locked dependencies and contract suites.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
