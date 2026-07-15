#!/usr/bin/env python3
"""Reject incomplete M04 external suites and issue sanitized database evidence."""

from __future__ import annotations

import argparse
import json
import os
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[2]
EXTERNAL_CLASS = "io.zartra.bedwars.storage.sql.ExternalSqlStorageContractTest"
MANDATORY_EXTERNAL_TESTS = 12
REQUIRED_QUERY_IDS = {
    "record-primary-key", "outbox-claim-index", "inbox-primary-key",
    "retention-primary-key", "legal-hold-primary-key", "tombstone-primary-key",
    "migration-history-primary-key",
}


def integer(root: ET.Element, name: str) -> int:
    return int(root.attrib.get(name, "0"))


def test_totals() -> tuple[dict[str, int], dict[str, int]]:
    overall = {key: 0 for key in ("tests", "failures", "errors", "skipped")}
    external = dict(overall)
    reports = sorted(ROOT.glob("**/target/surefire-reports/TEST-*.xml"))
    if not reports:
        raise ValueError("no Surefire XML reports were generated")
    for report in reports:
        suite = ET.parse(report).getroot()
        values = {key: integer(suite, key) for key in overall}
        for key, value in values.items():
            overall[key] += value
        if suite.attrib.get("name") == EXTERNAL_CLASS:
            for key, value in values.items():
                external[key] += value
    return overall, external


def read_json(path: Path) -> dict[str, object]:
    if not path.is_file():
        raise ValueError(f"missing mandatory evidence file: {path}")
    return json.loads(path.read_text(encoding="utf-8"))


def validate_evidence(engine: str, evidence: Path) -> dict[str, object]:
    identity = read_json(evidence / "database-identity.json")
    plans = read_json(evidence / "query-plans.json")
    pool = read_json(evidence / "pool-health.json")
    backup = read_json(evidence / "backup-restore.json")
    if identity.get("engine") != engine or identity.get("status") != "CERTIFIED":
        raise ValueError("database identity is absent or uncertified")
    if not re.fullmatch(r"[0-9]+(?:\.[0-9]+)+(?:[-+].*)?", str(identity.get("server_version", ""))):
        raise ValueError("database server version evidence is malformed")
    query_rows = plans.get("queries", [])
    if {row.get("id") for row in query_rows} != REQUIRED_QUERY_IDS:
        raise ValueError("query-plan evidence set is incomplete")
    for row in query_rows:
        if not row.get("uses_expected_index") or row.get("full_table_access"):
            raise ValueError(f"query plan is uncertified: {row.get('id')}")
        if not str(row.get("explain_json", "")).strip().startswith("{"):
            raise ValueError(f"query plan is not JSON: {row.get('id')}")
    allowed_pool = {"engine", "active", "idle", "total", "waiting", "maximum", "saturated", "status"}
    if set(pool) != allowed_pool or pool.get("status") != "CERTIFIED":
        raise ValueError("pool-health evidence contains missing or unapproved fields")
    if int(pool["total"]) > int(pool["maximum"]) or int(pool["maximum"]) != 4:
        raise ValueError("HikariCP pool exceeded the certified maximum")
    if backup.get("status") != "CERTIFIED" or not re.fullmatch(
            r"[0-9a-f]{64}", str(backup.get("sha256", ""))):
        raise ValueError("backup/restore evidence is incomplete")
    serialized = "\n".join(
        path.read_text(encoding="utf-8") for path in sorted(evidence.glob("*.json")))
    forbidden = ("jdbc:", "password", "username", "test-secret")
    if any(token.lower() in serialized.lower() for token in forbidden):
        raise ValueError("evidence contains a forbidden credential or connection field")
    secret = os.environ.get("ZBW_TEST_DATABASE_PASSWORD", "")
    if secret and secret in serialized:
        raise ValueError("evidence contains the temporary database credential")
    return identity


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--engine", choices=("mysql", "mariadb"), required=True)
    parser.add_argument("--evidence", type=Path, required=True)
    arguments = parser.parse_args()
    try:
        overall, external = test_totals()
        if external != {"tests": MANDATORY_EXTERNAL_TESTS, "failures": 0,
                        "errors": 0, "skipped": 0}:
            raise ValueError(f"mandatory external-suite totals are incomplete: {external}")
        if overall["failures"] or overall["errors"] or overall["skipped"]:
            raise ValueError(f"complete M04 reactor contains a failure/error/skip: {overall}")
        identity = validate_evidence(arguments.engine, arguments.evidence)
        certification = {
            "schema_version": 1,
            "engine": arguments.engine,
            "server_version": identity["server_version"],
            "image_reference": identity["image_reference"],
            "reactor_tests": overall,
            "mandatory_external_tests": external,
            "query_plan_count": len(REQUIRED_QUERY_IDS),
            "status": "CERTIFIED",
        }
        output = arguments.evidence / "certification.json"
        output.write_text(json.dumps(certification, indent=2, sort_keys=True) + "\n", encoding="utf-8")
        print(
            f"{arguments.engine} M04 certification PASS: {overall['tests']} reactor tests, "
            f"{external['tests']} mandatory external contracts, zero skipped.")
        return 0
    except (OSError, ValueError, json.JSONDecodeError, ET.ParseError) as exception:
        print(f"ERROR: {exception}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
