# M09 administrator interaction guide

Use `/zbw arena …` for arena lifecycle and diagnostics, `/zbw setup …` for the typed setup session,
and `/zbw game …` for authorized M08 session operations. Addon presentation families use
`/zbw hotbar-manager`, `arena-start-message`, `anti-drop`, `leave-delay`, `tab-sorter`, `bossbar`
and `adventure-mode`; player deposit operations use `/deposit`.

Every command has the equivalent generated GUI page listed in `COMMANDS.md`. Search/filter/sort
never changes authorization. Destructive operations show a preview and issue a short-lived token;
review actor, target and revision, then apply once. A replayed or stale token is rejected and audited.
Use validation and diagnostics before enable/apply. Errors are localized keys with safe parameters;
server logs receive correlation IDs, not secrets or raw imported content.
