# M06 primary Paper bootstrap

## Certified runtime

The only M06 runtime claim is Paper 1.21.1 build 133 on Temurin 21.0.6+7.
The server SHA-256 is
`39bd8c00b9e18de91dcabd3cc3dcfa5328685a53b7187a2f63280c22e2d287b9`.
The compile API is the immutable local mirror
`io.zartra.mirror.paper:paper-api:1.21.1-build133`; it is `provided`, not
bundled. The server fixture is downloaded only for certification and is never
committed or redistributed.

## Lifecycle and configuration

`ZartraBedWarsPlugin` starts at Paper `STARTUP`, validates `config.yml`, creates
the bounded runtime and exposes no commands or gameplay listener. Configuration
bounds are:

| Key | Range | Default |
|---|---:|---:|
| `world.worker-count` | 1–8 | 2 |
| `world.queue-capacity` | 1–256 | 32 |
| `world.maximum-in-flight` | 1–64 | 8 |
| `world.maximum-tracked` | in-flight value–256 | 64 |
| `world.operation-timeout-seconds` | whole seconds 1–300 | 120 |

Owner dispatch rejects cancellation after terminal completion, converts
mutation exceptions to secret-safe failure reports and refuses to execute a
mutation after its descriptor deadline. Runtime shutdown stops admission and
world work immediately, then waits for worker termination outside the Paper
primary thread.

## Certification

Setting `ZBW_M06_CERTIFY=true` activates the otherwise dormant certification
sequence. `tools/validation/m06_paper_e2e.py` verifies the server hash, launches
an isolated server, requires five successful lifecycle operations, checks the
thread/leak/shutdown evidence, requires process exit 0 and emits
`build/evidence/m06-paper-primary.json`. CI uploads that evidence and the server
console log. A missing server, timeout, skipped operation, wrong hash, missing
startup/shutdown marker, leak or non-zero process exit fails the workflow.

The artifact/module name does not certify other 1.20 or 1.21 releases. All
remaining server rows, translated clients, Bedrock and legacy fallbacks remain
M22 gates.
