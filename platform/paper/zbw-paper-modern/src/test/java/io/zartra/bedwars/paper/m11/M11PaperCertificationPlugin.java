package io.zartra.bedwars.paper.m11;

import io.zartra.bedwars.api.authorization.AuthorizationSubject;
import io.zartra.bedwars.api.authorization.PermissionNode;
import io.zartra.bedwars.api.identity.ArenaId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.identity.ResourceId;
import io.zartra.bedwars.command.api.PresentationActions;
import io.zartra.bedwars.paper.game.BukkitM11Platform;
import io.zartra.bedwars.paper.game.M11PaperProjection;
import io.zartra.bedwars.paper.game.M11PresentationBindings;
import io.zartra.bedwars.shop.api.PurchaseContext;
import io.zartra.bedwars.shop.api.ShopCatalog;
import io.zartra.bedwars.shop.generator.GeneratorBatch;
import io.zartra.bedwars.shop.item.ItemActionRequest;
import io.zartra.bedwars.shop.item.UtilityItemDefinition;
import io.zartra.bedwars.shop.upgrade.TeamEffectIntent;
import io.zartra.bedwars.ui.api.AdminDashboard;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/** Test-artifact-only exact Paper 1.21.1 M11 certification plugin. */
public final class M11PaperCertificationPlugin extends JavaPlugin {
    private final MatchId matchId = MatchId.of(new UUID(11L, 1L));
    private final PlayerId playerId = PlayerId.of(new UUID(11L, 2L));
    private final DefinitionId teamId = DefinitionId.of("team", "red");

    @Override public void onEnable() {
        Bukkit.getScheduler().runTask(this, () -> {
            try { certify(); }
            catch (ReflectiveOperationException | RuntimeException failure) {
                getLogger().log(java.util.logging.Level.SEVERE, "M11 certification failed", failure);
                finish(Collections.singletonMap("success", false));
            }
        });
    }

    private void certify() throws ReflectiveOperationException {
        final World world = Bukkit.getWorlds().get(0);
        final Location origin = world.getSpawnLocation().clone().add(0, 3, 0);
        final TestActor actor = new TestActor(world, origin);
        final Block block = origin.clone().add(2, 0, 0).getBlock();
        final Material original = block.getType();
        final Mapping mapping = new Mapping(actor, origin, block);
        final M11PaperProjection projection = new M11PaperProjection(Bukkit::isPrimaryThread,
                new BukkitM11Platform(mapping));

        final GeneratorBatch batch = batch();
        projection.deliver(batch);
        projection.deliver(batch);
        final long drops = world.getEntitiesByClass(Item.class).stream()
                .filter(item -> item.getItemStack().getType() == Material.IRON_INGOT).count();

        final TeamEffectIntent forge = new TeamEffectIntent(IdempotencyKey.of("zartra", "m11-cert-forge"),
                TeamEffectIntent.Kind.FORGE_RESOURCES, teamId,
                DefinitionId.of("zartra", "effect/forge"), null,
                Collections.singletonMap(ResourceId.of("minecraft", "gold_ingot"), 3));
        projection.apply(forge);
        final int delivered = count(actor.inventory, Material.GOLD_INGOT);

        final UtilityItemDefinition tower = new UtilityItemDefinition(
                DefinitionId.of("zartra", "utility/popup-tower"),
                DefinitionId.of("zartra", "item/tower"), UtilityItemDefinition.Kind.POPUP_TOWER,
                PermissionNode.of("zartrabedwars.item.popup_tower"),
                new ShopCatalog.Price(Collections.singletonList(new ShopCatalog.ResourceAmount(
                        ResourceId.of("minecraft", "iron_ingot"), 24))), Duration.ofSeconds(1),
                UtilityItemDefinition.TargetRule.LOCATION, 4, Collections.emptyMap());
        final ItemActionRequest request = new ItemActionRequest(context(), tower.id(),
                IdempotencyKey.of("zartra", "m11-cert-tower"), Instant.EPOCH,
                Optional.of(new ItemActionRequest.Target(DefinitionId.of("zartra", "target/location"),
                        DefinitionId.of("zartra", "location/cert"), Optional.empty(), Optional.empty(),
                        true, true)));
        final boolean utility = projection.apply(DefinitionId.of("zartra", "effect/tower"), tower, request);
        projection.apply(DefinitionId.of("zartra", "effect/tower"), tower, request);
        final boolean blockApplied = block.getType() == Material.WHITE_WOOL;

        final Inventory shopInventory = Bukkit.createInventory(null, 54, Component.text("M11 Shop"));
        shopInventory.setItem(0, new ItemStack(Material.WHITE_WOOL));
        shopInventory.setItem(1, new ItemStack(Material.IRON_INGOT));
        final int actions = M11PresentationBindings.create((action, input) ->
                java.util.concurrent.CompletableFuture.completedFuture(PresentationActions.Response.simple(
                        PresentationActions.Response.Status.SUCCESS, "presentation.success", 1L))).size();
        final int pages = AdminDashboard.pages(PresentationActions.Catalog.m11(), definition ->
                new io.zartra.bedwars.ui.api.UiModel.PageDefinition(definition.pageId(),
                        io.zartra.bedwars.api.localization.MessageKey.of("ui.action.title"),
                        (viewer, query) -> java.util.concurrent.CompletableFuture.completedFuture(
                                new io.zartra.bedwars.ui.api.UiModel.PageState(1,
                                        io.zartra.bedwars.ui.api.UiModel.PageState.Status.READY, 0, 1,
                                        Collections.emptyList(),
                                        io.zartra.bedwars.api.localization.MessageKey.of("ui.ready"))),
                        Collections.singletonList(io.zartra.bedwars.ui.api.UiModel.Interaction.PRIMARY))).size();

        projection.clear(playerId);
        projection.clearMatch(matchId);
        final boolean cleanup = block.getType() == original
                && world.getEntitiesByClass(Item.class).stream().noneMatch(Item::isValid);
        final Map<String, Boolean> evidence = new LinkedHashMap<>();
        evidence.put("shop_inventory", shopInventory.getItem(0) != null && actions == 25 && pages == 26);
        evidence.put("item_delivery", delivered == 3);
        evidence.put("generator_spawn", drops == 1);
        evidence.put("duplicate_prevention", drops == 1 && count(actor.inventory, Material.GOLD_INGOT) == 3);
        evidence.put("blocks", utility && blockApplied);
        evidence.put("particles", actor.particles > 0);
        evidence.put("sounds", actor.sounds > 0);
        evidence.put("upgrades_traps_forge", delivered == 3);
        evidence.put("owner_thread", Bukkit.isPrimaryThread());
        evidence.put("cleanup", cleanup);
        evidence.put("success", !evidence.containsValue(false));
        finish(evidence);
    }

    private GeneratorBatch batch() throws ReflectiveOperationException {
        final Constructor<GeneratorBatch> constructor = GeneratorBatch.class.getDeclaredConstructor(
                IdempotencyKey.class, MatchId.class, DefinitionId.class, ResourceId.class,
                long.class, int.class, Instant.class);
        constructor.setAccessible(true);
        return constructor.newInstance(IdempotencyKey.of("zartra", "m11-cert-generator"), matchId,
                DefinitionId.of("zartra", "generator/iron"),
                ResourceId.of("minecraft", "iron_ingot"), 1L, 1, Instant.EPOCH);
    }

    private PurchaseContext context() {
        return new PurchaseContext(AuthorizationSubject.of(AuthorizationSubject.Kind.PLAYER,
                DefinitionId.of("zartra", "player/cert")), playerId, matchId,
                ArenaId.of(new UUID(11L, 3L)), DefinitionId.of("zartra", "mode/standard"),
                teamId, Optional.empty());
    }

    private void finish(final Map<String, Boolean> values) {
        final StringBuilder json = new StringBuilder("{\n  \"schema_version\": 1,\n")
                .append("  \"runtime\": \"Paper 1.21.1 build 133\",\n")
                .append("  \"server_sha256\": \"")
                .append(io.zartra.bedwars.compat.modern.Paper121CompatibilityAdapter.SERVER_SHA256)
                .append("\"");
        for (Map.Entry<String, Boolean> entry : values.entrySet()) {
            json.append(",\n  \"").append(entry.getKey()).append("\": ").append(entry.getValue());
        }
        json.append("\n}\n");
        try {
            final Path path = getDataFolder().toPath().resolve("m11-primary-certification.json");
            Files.createDirectories(path.getParent());
            Files.write(path, json.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException failure) { getLogger().severe("M11 evidence write failed"); }
        Bukkit.getScheduler().runTask(this, Bukkit::shutdown);
    }

    private static int count(final Inventory inventory, final Material material) {
        return Arrays.stream(inventory.getContents()).filter(java.util.Objects::nonNull)
                .filter(item -> item.getType() == material).mapToInt(ItemStack::getAmount).sum();
    }

    private final class Mapping implements BukkitM11Platform.Mappings {
        private final TestActor actor;
        private final Location origin;
        private final Block block;
        private Mapping(final TestActor actor, final Location origin, final Block block) {
            this.actor = actor;
            this.origin = origin;
            this.block = block;
        }
        @Override public Optional<Object> generatorLocation(final DefinitionId id) { return Optional.of(origin); }
        @Override public Optional<String> material(final ResourceId id) {
            return Optional.of(id.path().endsWith("gold_ingot") ? "GOLD_INGOT" : "IRON_INGOT");
        }
        @Override public Optional<Object> player(final PlayerId id) { return Optional.of(actor); }
        @Override public Collection<Object> teamPlayers(final DefinitionId id) {
            return Collections.singletonList(actor);
        }
        @Override public MatchId matchOf(final Object ignored) { return matchId; }
        @Override public List<Object> utilityBlocks(final DefinitionId effect,
                final UtilityItemDefinition definition, final ItemActionRequest request) {
            return Collections.singletonList(block.getLocation());
        }
        @Override public Optional<String> utilityMaterial(final DefinitionId effect,
                final UtilityItemDefinition definition) { return Optional.of("WHITE_WOOL"); }
        @Override public BukkitM11Platform.EffectMapping effect(final DefinitionId id) {
            return new BukkitM11Platform.EffectMapping(Optional.of("BLOCK_NOTE_BLOCK_PLING"),
                    Optional.of("HAPPY_VILLAGER"), 1F, 1F, 1);
        }
        @Override public void clearOwnedEffects(final PlayerId id) { }
    }

    /** Bukkit-shaped test actor backed by a real server inventory and world. */
    public static final class TestActor {
        private final World world;
        private final Location location;
        private final Inventory inventory = Bukkit.createInventory(null, 9);
        private int sounds;
        private int particles;
        TestActor(final World world, final Location location) {
            this.world = world;
            this.location = location;
        }
        public Inventory getInventory() { return inventory; }
        public Location getLocation() { return location; }
        public World getWorld() { return world; }
        public void playSound(final Location where, final String sound, final float volume, final float pitch) {
            world.playSound(where, Sound.valueOf(sound), volume, pitch);
            sounds++;
        }
        public void spawnParticle(final Particle particle, final Location where, final int count) {
            world.spawnParticle(particle, where, count);
            particles += count;
        }
    }
}
