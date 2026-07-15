# M03 authorization and permission foundation

M03 implements the centralized authorization boundary required by `ZBW-UX-004`. All protected use cases must submit an `AuthorizationRequest`; adapters may not duplicate policy. Decisions contain an explicit allow/deny outcome and stable reason and are sent to the injected sanitized audit sink.

Permission nodes use the `zartrabedwars.<resource>.<action>` namespace. The canonical action inventory is:

`view`, `use`, `create`, `edit`, `delete`, `duplicate`, `import`, `export`, `enable`, `disable`, `start`, `stop`, `force`, `reload`, `reset`, `backup`, `restore`, `migrate`, `inspect`, `debug`, `bypass`, `manage`, `grant`, `revoke`, `set`, `add`, `remove`, `approve`, `reject`, `override`, `view.identity`, `view.hidden` and `view.private`.

Authorization is least-privilege and exact-match. A grant may optionally be scoped to a typed target. There is no implicit wildcard, parent inheritance, operator bypass or role-derived authority. Role names are documentation labels only. Deprecated aliases are explicit, acyclic and limited to one hop.

The RESOURCE SCARCITY management node is `zartrabedwars.private.resource-scarcity.manage`. Configuration, localization and security consumers use canonical nodes under their own resource families. The later command and GUI inventories must map each action to these same nodes; M03 introduces no commands or GUIs.

Sensitive grants, revocations, overrides and private/hidden/identity reads must be audited with operation, subject, target, outcome and safe reason. Audit records must never contain credentials, localized private text or resolved secrets. Concrete persistence, staff role mapping, LuckPerms integration, command inspection and permission GUIs remain their assigned later milestones.
