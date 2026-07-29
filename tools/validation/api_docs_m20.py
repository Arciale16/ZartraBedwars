#!/usr/bin/env python3
"""Generate strict deterministic Java 8 documentation for M20 Proxy API."""
from pathlib import Path
import api_docs
ROOT=Path(__file__).resolve().parents[2]
def main():
    """Document neutral proxy contracts without platform adapters."""
    sources=api_docs.sources(("proxy/zbw-proxy-api",));output=ROOT/"target/apidocs-m20-proxy-api"
    result=api_docs.generate(api_docs.executable("8","1.8.0_442"),"8",sources,output,api_docs.artifacts(("api/zbw-api","proxy/zbw-proxy-api")))
    if result:return result
    api_docs.archive(output,ROOT/"target/zartrabedwars-m20-proxy-api-javadoc.zip");print(f"M20 Proxy JavaDoc PASS: {len(sources)} Java 8 sources");return 0
if __name__=="__main__":raise SystemExit(main())