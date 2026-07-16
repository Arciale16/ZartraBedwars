# Milestone 8 typed API

`zbw-game` exports immutable Java-8 types in `io.zartra.bedwars.game.model`,
`application`, `spi` and `addon`. Public methods reject null or malformed input,
return typed `Result`/immutable outcomes, and do not expose Bukkit, Paper, storage,
filesystem, Redis, proxy or runtime-configuration types.

The model is thread-safe by immutability; `MatchStateMachine` and policy registries
serialize mutation. Repository/event/projection ports document whether completion may
occur off-thread. `PlayerProjection` effects require the platform owner thread.

M08 is additive. Consumers must use stable identity and semantic value types, must not
persist implementation classes, and must tolerate additive enum/API growth according
to the project versioning policy. M09 may adapt these use cases but may not move game
rules into commands, GUIs, editors or confirmation flows.
