package io.zartra.bedwars.paper.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.zartra.bedwars.command.api.PresentationActions;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

final class M12PresentationBindingsTest {
    @Test
    void bindsEveryM12ActionWithoutOwningBusinessRules() {
        final AtomicInteger calls = new AtomicInteger();
        final Map<PresentationActions.ActionId, PresentationActions.UseCase> bindings =
                M12PresentationBindings.create((action, request) -> {
                    calls.incrementAndGet();
                    return CompletableFuture.completedFuture(PresentationActions.Response.simple(
                            PresentationActions.Response.Status.SUCCESS, "m12.success",
                            request.revision()));
                });
        assertEquals(17, bindings.size());
        assertThrows(UnsupportedOperationException.class,
                () -> bindings.clear());
        assertEquals(0, calls.get());
    }

    @Test
    void rejectsMissingOperations() {
        assertThrows(NullPointerException.class, () -> M12PresentationBindings.create(null));
    }
}
