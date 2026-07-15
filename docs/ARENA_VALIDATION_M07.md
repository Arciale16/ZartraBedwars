# M07 arena validation and enable gating

Validation is deterministic, side-effect free and returns every actionable
issue sorted by field path and stable code. Messages are localization keys;
arbitrary untrusted text is not emitted.

The built-in rule set checks:

- active/template world, waiting spawn, spectator spawn and playable bounds;
- at least two teams, total team capacity, each team spawn and bed/facing;
- team, diamond and emerald generators and valid team ownership;
- shop and upgrade NPCs for each team and valid team ownership;
- all spawn, bed, generator and NPC positions against bounds, void and build
  limits;
- protected regions inside playable bounds;
- block-coordinate collisions among spawns, beds, generators and NPCs.

Any `ERROR` blocks enable, enabled import/restore and last-known-good promotion.
Draft validation does not mutate the session. Final commit revalidates the
candidate so presentation or provider latency cannot bypass the gate.

M09 renders reports and navigation; M16 may expose authorized validation
fields; M22 certifies legacy visual fallbacks. None of those later surfaces may
weaken the M07 error policy.
