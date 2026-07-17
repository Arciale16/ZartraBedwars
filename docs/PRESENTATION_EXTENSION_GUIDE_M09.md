# M09 command and GUI extension guide

An extension declares a stable action ID, canonical command path, stable page ID, exact permission,
destructive flag and Requirement IDs. It supplies one neutral `PresentationActions.UseCase` binding
and one page loader. Register both during bootstrap; unknown, missing and duplicate bindings fail.

Use typed arguments and structured responses. Never retain a sender, player, inventory or event in
neutral code. Load external state asynchronously through a bounded service, honor cancellation and
deadline, and schedule only the final Paper mutation on the owner thread. A destructive action must
use the shared confirmation framework. Add parity, permission/error/confirmation/audit tests and
regenerate both inventories and the feature dashboard before publication.
