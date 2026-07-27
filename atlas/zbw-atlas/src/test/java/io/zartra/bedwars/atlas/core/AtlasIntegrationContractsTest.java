package io.zartra.bedwars.atlas.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.Test;

/** ZBW-ATLAS-002/010/011 neutral integration ownership contract evidence. */
class AtlasIntegrationContractsTest {
    @Test void everyIntegrationBoundaryIsAsynchronousAndIntentOnly() {
        for (Class<?> type : AtlasIntegrationContracts.class.getDeclaredClasses()) {
            for (Method method : type.getDeclaredMethods()) {
                assertEquals(CompletionStage.class, method.getReturnType(), method.toString());
            }
        }
    }
}
