# M12 GUI surfaces

Every M12 command action has a parity page under `zartra:m12/...`. Player pages present progression,
XP, level, prestige, balances, rewards and entitlements. Admin pages present progression, reward,
currency, immutable ledger and failure/recovery evidence.

Pages use M09 bounded asynchronous loading, pagination, search/filter queries, accessible command
and keyboard alternatives, loading/empty/error states, stale result and stale click rejection,
duplicate-click protection and lifecycle cleanup. Mutations use the shared confirmation framework
and revalidate authorization and aggregate revision immediately before execution.

Paper only renders the resulting immutable page state on the owner thread. SQL, reward delivery
and progression calculations never run in inventory callbacks.
