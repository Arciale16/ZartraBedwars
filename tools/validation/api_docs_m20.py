#!/usr/bin/env python3
"""Generate strict deterministic Java 8/21 documentation for M20 Proxy."""
from pathlib import Path
import api_docs
ROOT=Path(__file__).resolve().parents[2]
def main():
    """Document neutral proxy runtime and both platform adapter boundaries."""
    neutral=api_docs.sources(("proxy/zbw-proxy-api","proxy/zbw-bungeecord"));neutral_output=ROOT/"target/apidocs-m20-proxy-java8"
    result=api_docs.generate(api_docs.executable("8","1.8.0_442"),"8",neutral,neutral_output,api_docs.artifacts(("api/zbw-api","proxy/zbw-proxy-api","proxy/zbw-bungeecord")))
    if result:return result
    modern=api_docs.sources(("proxy/zbw-velocity",));modern_output=ROOT/"target/apidocs-m20-proxy-java21"
    result=api_docs.generate(api_docs.executable("21","21.0.6"),"21",modern,modern_output,api_docs.artifacts(("api/zbw-api","proxy/zbw-proxy-api","proxy/zbw-velocity")))
    if result:return result
    api_docs.archive(neutral_output,ROOT/"target/zartrabedwars-m20-proxy-java8-javadoc.zip")
    api_docs.archive(modern_output,ROOT/"target/zartrabedwars-m20-proxy-java21-javadoc.zip")
    print(f"M20 Proxy JavaDoc PASS: {len(neutral)} Java 8 and {len(modern)} Java 21 sources");return 0
if __name__=="__main__":raise SystemExit(main())
