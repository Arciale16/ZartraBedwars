package io.zartra.bedwars.progression.catalog;

import io.zartra.bedwars.progression.achievement.AchievementDefinition;
import io.zartra.bedwars.progression.challenge.ChallengeDefinition;
import io.zartra.bedwars.progression.objective.ObjectiveDefinition;
import io.zartra.bedwars.progression.objective.ObjectiveId;
import io.zartra.bedwars.progression.pass.BattlePassDefinition;
import io.zartra.bedwars.progression.quest.QuestDefinition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Immutable M13 definition catalogue with deterministic reference validation. */
public final class M13Catalog {
    private final List<ObjectiveDefinition> objectives;
    private final List<QuestDefinition> quests;
    private final List<AchievementDefinition> achievements;
    private final List<ChallengeDefinition> challenges;
    private final List<BattlePassDefinition> seasons;

    /** Creates and validates an immutable catalogue snapshot. */
    public M13Catalog(final List<ObjectiveDefinition> objectives, final List<QuestDefinition> quests,
                      final List<AchievementDefinition> achievements,
                      final List<ChallengeDefinition> challenges,
                      final List<BattlePassDefinition> seasons) {
        this.objectives = immutable(objectives, "objectives");
        this.quests = immutable(quests, "quests");
        this.achievements = immutable(achievements, "achievements");
        this.challenges = immutable(challenges, "challenges");
        this.seasons = immutable(seasons, "seasons");
        validateReferences();
    }

    /** @return immutable objectives */ public List<ObjectiveDefinition> objectives() { return objectives; }
    /** @return immutable quests */ public List<QuestDefinition> quests() { return quests; }
    /** @return immutable achievements */
    public List<AchievementDefinition> achievements() { return achievements; }
    /** @return immutable challenges */ public List<ChallengeDefinition> challenges() { return challenges; }
    /** @return immutable battle-pass seasons */ public List<BattlePassDefinition> seasons() { return seasons; }

    private void validateReferences() {
        final Set<ObjectiveId> objectiveIds = new HashSet<ObjectiveId>();
        for (ObjectiveDefinition objective : objectives) {
            if (!objectiveIds.add(objective.id())) { throw new IllegalArgumentException("duplicate objective ID"); }
        }
        final Set<Object> definitionIds = new HashSet<Object>();
        for (QuestDefinition quest : quests) {
            requireUnique(definitionIds, quest.id(), "quest");
            requireObjective(objectiveIds, quest.objectiveId());
        }
        for (AchievementDefinition achievement : achievements) {
            requireUnique(definitionIds, achievement.id(), "achievement");
            requireObjective(objectiveIds, achievement.objectiveId());
        }
        for (ChallengeDefinition challenge : challenges) {
            requireUnique(definitionIds, challenge.id(), "challenge");
            requireObjective(objectiveIds, challenge.objectiveId());
        }
        for (BattlePassDefinition season : seasons) {
            requireUnique(definitionIds, season.id(), "season");
        }
        for (ObjectiveDefinition objective : objectives) {
            for (ObjectiveId child : objective.children()) { requireObjective(objectiveIds, child); }
        }
    }

    private static void requireObjective(final Set<ObjectiveId> ids, final ObjectiveId id) {
        if (!ids.contains(id)) { throw new IllegalArgumentException("unknown objective reference: " + id); }
    }

    private static void requireUnique(final Set<Object> ids, final Object id, final String type) {
        if (!ids.add(id)) { throw new IllegalArgumentException("duplicate " + type + " ID"); }
    }

    private static <T> List<T> immutable(final List<T> values, final String name) {
        final List<T> copy = new ArrayList<T>(Objects.requireNonNull(values, name));
        if (copy.contains(null)) { throw new IllegalArgumentException(name + " must not contain null"); }
        return Collections.unmodifiableList(copy);
    }
}
