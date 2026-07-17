# M09 accessibility and fallback guide

Every actionable component supplies a plain label, description and non-pointer interaction intent.
The neutral model supports primary, secondary and keyboard-equivalent interactions. A presentation
adapter must offer a localized command or chat-input route when inventory interaction is unavailable
or unreliable, including Bedrock-oriented flows. Search and pagination must remain reachable without
precise drag gestures; drags are cancelled to prevent item movement.

Modern color, item, sound and text rendering resolves through compatibility semantics. M09 certifies
Paper 1.21.1 only. M22 must map these semantics to legacy equivalents and document any purely
decorative suppression; gameplay actions, validation and authorization may never be suppressed.
