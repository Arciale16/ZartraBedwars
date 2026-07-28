#!/usr/bin/env python3
"""Generate strict deterministic Java 8 documentation for M19 Redis API."""
from pathlib import Path
import api_docs
ROOT=Path(__file__).resolve().parents[2]
def main():
    """Document Redis contracts against M04 storage contracts."""
    sources=api_docs.sources(("redis/zbw-redis-api",));output=ROOT/"target/apidocs-m19-redis-api"
    result=api_docs.generate(api_docs.executable("8","1.8.0_442"),"8",sources,output,api_docs.artifacts(("api/zbw-api","domain/zbw-domain","storage/zbw-storage-api","redis/zbw-redis-api")))
    if result:return result
    api_docs.archive(output,ROOT/"target/zartrabedwars-m19-redis-api-javadoc.zip");print(f"M19 Redis JavaDoc PASS: {len(sources)} Java 8 sources");return 0
if __name__=="__main__":raise SystemExit(main())