package io.zartra.bedwars.progression.quest;

import io.zartra.bedwars.api.identity.NamespacedIdentifier;

/** Stable typed identity of a quest definition. */
public final class QuestId extends NamespacedIdentifier {
    private QuestId(final String namespace, final String path) { super(namespace, path); }
    /** @return a validated quest identity */ public static QuestId of(final String namespace, final String path) {
        return new QuestId(namespace, path);
    }
    /** @return a parsed quest identity */ public static QuestId parse(final String value) {
        final String[] parts = split(value);
        return of(parts[0], parts[1]);
    }
}
