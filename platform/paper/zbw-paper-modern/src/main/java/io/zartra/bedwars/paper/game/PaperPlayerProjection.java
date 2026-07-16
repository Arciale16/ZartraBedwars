package io.zartra.bedwars.paper.game;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.game.addon.DepositPolicy;
import io.zartra.bedwars.game.addon.HotbarPolicy;
import io.zartra.bedwars.game.model.PlayerStateSnapshot;
import io.zartra.bedwars.game.spi.PlayerProjection;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Bukkit;

/** Exact Paper owner-thread player, inventory, hotbar and restoration projector. */
public final class PaperPlayerProjection implements PlayerProjection {
    private final WorldResolver worlds;
    private final ItemResolver items;

    /** Creates a projector with explicit version-safe world and item resolvers. */
    public PaperPlayerProjection(final WorldResolver worlds, final ItemResolver items) {
        this.worlds = Objects.requireNonNull(worlds, "worlds");
        this.items = Objects.requireNonNull(items, "items");
    }

    @Override public boolean isOwnerThread() { return Bukkit.isPrimaryThread(); }

    @Override public void apply(final PlayerId playerId, final PlayerView view) {
        final Object player = requirePlayer(playerId);
        final String mode;
        switch (Objects.requireNonNull(view, "view")) {
            case WAITING:
                mode = "ADVENTURE";
                break;
            case PLAYING:
                mode = "SURVIVAL";
                break;
            case SPECTATOR:
                mode = "SPECTATOR";
                break;
            case POST_GAME:
                mode = "ADVENTURE";
                break;
            default: throw new IllegalArgumentException("unknown player view");
        }
        setMode(player, mode);
    }

    @Override public void restore(final PlayerStateSnapshot capturedState) {
        requireOwner();
        final Object player = requirePlayer(capturedState.playerId());
        replace(PaperReflection.invoke(player, "getInventory", new Class<?>[0]),
                capturedState.inventory());
        final PlayerStateSnapshot.Location saved = capturedState.location();
        final Object world = Objects.requireNonNull(worlds.resolve(saved.worldId()),
                "world resolver returned null");
        if (!PaperReflection.WORLD.isInstance(world)) {
            throw new IllegalArgumentException("world resolver returned a non-World value");
        }
        final Object location = PaperReflection.construct(PaperReflection.LOCATION,
                new Class<?>[] {PaperReflection.WORLD, double.class, double.class, double.class,
                        float.class, float.class}, world, saved.x(), saved.y(), saved.z(),
                saved.yaw(), saved.pitch());
        PaperReflection.invoke(player, "teleport", new Class<?>[] {PaperReflection.LOCATION}, location);
        setMode(player, capturedState.mode().name());
        PaperReflection.invoke(player, "setInvisible", new Class<?>[] {boolean.class},
                Boolean.valueOf(!capturedState.visible()));
        update(player);
    }

    @Override public void clear(final PlayerId playerId) {
        requireOwner();
        final Object player = find(playerId);
        if (player != null) {
            PaperReflection.invoke(PaperReflection.invoke(player, "getInventory", new Class<?>[0]),
                    "clear", new Class<?>[0]);
            update(player);
        }
    }

    /** Replaces only the nine owned slots with a resolved immutable loadout. */
    public void applyHotbar(final PlayerId playerId, final HotbarPolicy.Loadout loadout) {
        final Object player = requirePlayer(playerId);
        final Object inventory = PaperReflection.invoke(player, "getInventory", new Class<?>[0]);
        for (int slot = 0; slot < 9; slot++) { setItem(inventory, slot, null); }
        for (Map.Entry<Integer, HotbarPolicy.Slot> entry : loadout.slots().entrySet()) {
            setItem(inventory, entry.getKey().intValue(), requireItem(entry.getValue().item()));
        }
        update(player);
    }

    /** Applies both sides of an accepted deposit outcome in one owner-thread turn. */
    public void applyDeposit(final PlayerId playerId, final DepositPolicy.Outcome outcome) {
        if (!outcome.accepted()) { throw new IllegalArgumentException("deposit outcome was rejected"); }
        final Object player = requirePlayer(playerId);
        replace(PaperReflection.invoke(player, "getInventory", new Class<?>[0]), outcome.source());
        replace(PaperReflection.invoke(player, "getEnderChest", new Class<?>[0]), outcome.enderChest());
        update(player);
    }

    private void replace(final Object target, final PlayerStateSnapshot.Inventory source) {
        requireOwner();
        final int size = ((Integer) PaperReflection.invoke(target, "getSize", new Class<?>[0])).intValue();
        if (source.size() > size) { throw new IllegalArgumentException("neutral inventory exceeds platform inventory"); }
        PaperReflection.invoke(target, "clear", new Class<?>[0]);
        for (Map.Entry<Integer, PlayerStateSnapshot.Item> entry : source.occupied().entrySet()) {
            setItem(target, entry.getKey().intValue(), requireItem(entry.getValue()));
        }
    }

    private Object requireItem(final PlayerStateSnapshot.Item item) {
        final Object resolved = items.resolve(item);
        if (resolved == null || !PaperReflection.ITEM_STACK.isInstance(resolved)) {
            throw new IllegalArgumentException("item resolver returned a non-ItemStack value");
        }
        final int amount = ((Integer) PaperReflection.invoke(resolved, "getAmount", new Class<?>[0])).intValue();
        final Object material = PaperReflection.invoke(resolved, "getType", new Class<?>[0]);
        final boolean air = ((Boolean) PaperReflection.invoke(material, "isAir", new Class<?>[0])).booleanValue();
        if (air || amount != item.amount()) { throw new IllegalArgumentException("item resolver returned an unsafe stack"); }
        return PaperReflection.invoke(resolved, "clone", new Class<?>[0]);
    }

    private static void setItem(final Object inventory, final int slot, final Object item) {
        PaperReflection.invoke(inventory, "setItem",
                new Class<?>[] {int.class, PaperReflection.ITEM_STACK}, Integer.valueOf(slot), item);
    }
    private static void setMode(final Object player, final String mode) {
        PaperReflection.invoke(player, "setGameMode", new Class<?>[] {PaperReflection.GAME_MODE},
                PaperReflection.constant(PaperReflection.GAME_MODE, mode));
    }
    private static void update(final Object player) { PaperReflection.invoke(player, "updateInventory", new Class<?>[0]); }
    private static Object requirePlayer(final PlayerId playerId) {
        requireOwner();
        final Object player = find(playerId);
        if (player == null) { throw new IllegalStateException("player is not online"); }
        return player;
    }
    private static Object find(final PlayerId playerId) {
        return PaperReflection.invokeStatic(Bukkit.class, "getPlayer", new Class<?>[] {UUID.class},
                Objects.requireNonNull(playerId, "playerId").asUuid());
    }
    private static void requireOwner() {
        if (!Bukkit.isPrimaryThread()) { throw new IllegalStateException("Paper mutation requires primary thread"); }
    }

    /** Resolves semantic world identities to an already-loaded Paper World object. */
    public interface WorldResolver { /** @return runtime World object or null when unavailable */ Object resolve(DefinitionId worldId); }
    /** Resolves semantic immutable items to an exact-amount Paper ItemStack object. */
    public interface ItemResolver { /** @return runtime ItemStack object or null when unsupported */ Object resolve(PlayerStateSnapshot.Item item); }
}
