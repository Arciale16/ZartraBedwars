package io.zartra.bedwars.paper.replay.visual;

import io.zartra.bedwars.replay.api.ReplayEvent;
import io.zartra.bedwars.replay.playback.PlaybackSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Pure Paper-side visual reconstruction from the playback timeline and cursor.
 *
 * <p>The engine never advances playback and therefore cannot duplicate replay lifecycle logic.</p>
 */
public final class ReplayVisualEngine {
    private static final double DEFAULT_HEALTH = 20.0D;
    private final int maximumEntities;
    private final int maximumImportantEvents;

    /** Creates a bounded visual engine. */
    public ReplayVisualEngine(final int maximumEntities, final int maximumImportantEvents) {
        if (maximumEntities < 1 || maximumImportantEvents < 1) {
            throw new IllegalArgumentException("visual bounds must be positive");
        }
        this.maximumEntities = maximumEntities;
        this.maximumImportantEvents = maximumImportantEvents;
    }

    /** Rebuilds visual state through the playback session's inclusive cursor. */
    public ReplayVisualResult reconstruct(final PlaybackSession playback) {
        Objects.requireNonNull(playback, "playback");
        final int target = playback.cursor().position().eventIndex();
        if (target < -1 || target >= playback.timeline().events().size()) {
            return ReplayVisualResult.rejected(ReplayVisualResult.Status.CORRUPT);
        }
        final Map<String, VisualEntityState> entities =
                new TreeMap<String, VisualEntityState>();
        final List<VisualMatchEvent> important = new ArrayList<VisualMatchEvent>();
        try {
            for (int index = 0; index <= target; index++) {
                final ReplayEvent event = playback.timeline().events().get(index);
                if (event.sequence() != index) {
                    return ReplayVisualResult.rejected(ReplayVisualResult.Status.CORRUPT);
                }
                apply(event, entities, important);
                if (entities.size() > maximumEntities) {
                    return ReplayVisualResult.rejected(
                            ReplayVisualResult.Status.OVER_CAPACITY);
                }
            }
            return ReplayVisualResult.success(ReplayVisualResult.Status.APPLIED,
                    new ReplayVisualState(target, entities, important));
        } catch (IllegalArgumentException malformed) {
            return ReplayVisualResult.rejected(ReplayVisualResult.Status.CORRUPT);
        }
    }

    private void apply(final ReplayEvent event,
                       final Map<String, VisualEntityState> entities,
                       final List<VisualMatchEvent> important) {
        final String type = normalize(event.type());
        if (is(type, "SPAWN", "PLAYER_SPAWN", "MOVE", "MOVEMENT", "PLAYER_MOVE",
                "POSITION", "POSITION_SNAPSHOT")) {
            applyPosition(event, entities);
        } else if (is(type, "HEALTH", "HEALTH_CHANGE", "STATE_SNAPSHOT")) {
            applyHealth(event, entities);
        } else if (is(type, "EQUIPMENT", "EQUIPMENT_CHANGE", "INVENTORY",
                "INVENTORY_CHANGE", "ITEM_CHANGE")) {
            applyEquipment(event, entities);
        } else if (is(type, "DEATH", "PLAYER_DEATH")) {
            applyDeath(event, entities);
            appendImportant(event, subject(event.attributes()), important);
        } else if (is(type, "KILL", "FINAL_KILL", "PLAYER_KILL")) {
            final String victim = first(event.attributes(), "victim_id", "victim");
            if (victim != null && entities.containsKey(victim)) {
                final VisualEntityState current = entities.get(victim);
                entities.put(victim, current.health(0.0D, false));
            }
            appendImportant(event, victim == null ? subject(event.attributes()) : victim,
                    important);
        } else if (is(type, "BED_DESTRUCTION", "BED_DESTROYED", "BED_BREAK",
                "MATCH_START", "MATCH_END", "TEAM_ELIMINATED")) {
            appendImportant(event, subject(event.attributes()), important);
        } else if ("true".equalsIgnoreCase(event.attributes().get("important"))) {
            appendImportant(event, subject(event.attributes()), important);
        }
    }

    private void applyPosition(final ReplayEvent event,
                               final Map<String, VisualEntityState> entities) {
        final Map<String, String> attributes = event.attributes();
        final String entityId = requireSubject(attributes);
        final VisualPosition position = new VisualPosition(
                required(attributes, "world"),
                number(attributes, "x"), number(attributes, "y"), number(attributes, "z"),
                decimal(attributes, "yaw", 0.0F), decimal(attributes, "pitch", 0.0F));
        final VisualEntityState current = entities.get(entityId);
        if (current == null) {
            final String displayName = defaulted(
                    first(attributes, "display_name", "player_name", "name"), entityId);
            entities.put(entityId, new VisualEntityState(entityId, displayName, position,
                    equipment(attributes), decimal(attributes, "health", DEFAULT_HEALTH),
                    flag(attributes, "alive", true)));
        } else {
            entities.put(entityId, current.move(position));
        }
    }

    private static void applyHealth(final ReplayEvent event,
                                    final Map<String, VisualEntityState> entities) {
        final String entityId = requireSubject(event.attributes());
        final VisualEntityState current = requiredEntity(entities, entityId);
        final double health = number(event.attributes(), "health");
        entities.put(entityId, current.health(health,
                flag(event.attributes(), "alive", health > 0.0D)));
    }

    private static void applyEquipment(final ReplayEvent event,
                                       final Map<String, VisualEntityState> entities) {
        final Map<String, String> attributes = event.attributes();
        final String entityId = requireSubject(attributes);
        final VisualEntityState current = requiredEntity(entities, entityId);
        final String slot = required(attributes, "slot");
        final String item = defaulted(first(attributes, "item", "material"), "AIR");
        entities.put(entityId, current.equip(slot, item));
    }

    private static void applyDeath(final ReplayEvent event,
                                   final Map<String, VisualEntityState> entities) {
        final String entityId = requireSubject(event.attributes());
        final VisualEntityState current = requiredEntity(entities, entityId);
        entities.put(entityId, current.health(0.0D, false));
    }

    private void appendImportant(final ReplayEvent event, final String subject,
                                 final List<VisualMatchEvent> important) {
        important.add(new VisualMatchEvent(event.sequence(), event.offsetMillis(),
                normalize(event.type()), subject));
        if (important.size() > maximumImportantEvents) {
            important.remove(0);
        }
    }

    private static VisualEquipmentState equipment(final Map<String, String> attributes) {
        final Map<String, String> items = new TreeMap<String, String>();
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            if (entry.getKey().startsWith("equipment.")) {
                items.put(entry.getKey().substring("equipment.".length()), entry.getValue());
            }
        }
        return new VisualEquipmentState(items);
    }

    private static VisualEntityState requiredEntity(
            final Map<String, VisualEntityState> entities, final String entityId) {
        final VisualEntityState state = entities.get(entityId);
        if (state == null) { throw new IllegalArgumentException("visual entity is missing"); }
        return state;
    }

    private static String requireSubject(final Map<String, String> attributes) {
        final String value = first(attributes, "entity_id", "player_id", "player", "actor");
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("visual subject is missing");
        }
        return value;
    }

    private static String subject(final Map<String, String> attributes) {
        final String value = first(attributes, "entity_id", "player_id", "player", "actor",
                "team_id", "team", "subject");
        return defaulted(value, "match");
    }

    private static String required(final Map<String, String> attributes, final String key) {
        final String value = attributes.get(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("required visual attribute is missing");
        }
        return value;
    }

    private static double number(final Map<String, String> attributes, final String key) {
        final double value = Double.parseDouble(required(attributes, key));
        if (!Double.isFinite(value)) { throw new IllegalArgumentException("number is not finite"); }
        return value;
    }

    private static double decimal(final Map<String, String> attributes, final String key,
                                  final double defaultValue) {
        final String value = attributes.get(key);
        if (value == null) { return defaultValue; }
        final double parsed = Double.parseDouble(value);
        if (!Double.isFinite(parsed)) { throw new IllegalArgumentException("number is not finite"); }
        return parsed;
    }

    private static float decimal(final Map<String, String> attributes, final String key,
                                 final float defaultValue) {
        final String value = attributes.get(key);
        if (value == null) { return defaultValue; }
        final float parsed = Float.parseFloat(value);
        if (!Float.isFinite(parsed)) { throw new IllegalArgumentException("number is not finite"); }
        return parsed;
    }

    private static boolean flag(final Map<String, String> attributes, final String key,
                                final boolean defaultValue) {
        final String value = attributes.get(key);
        if (value == null) { return defaultValue; }
        if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
            throw new IllegalArgumentException("boolean is malformed");
        }
        return Boolean.parseBoolean(value);
    }

    private static String first(final Map<String, String> attributes,
                                final String... keys) {
        for (String key : keys) {
            final String value = attributes.get(key);
            if (value != null) { return value; }
        }
        return null;
    }

    private static String defaulted(final String value, final String defaultValue) {
        return value == null || value.trim().isEmpty() ? defaultValue : value;
    }

    private static String normalize(final String value) {
        return value.trim().toUpperCase(Locale.ROOT).replace('.', '_')
                .replace('-', '_').replace(':', '_');
    }

    private static boolean is(final String value, final String... candidates) {
        for (String candidate : candidates) {
            if (candidate.equals(value)) { return true; }
        }
        return false;
    }
}
