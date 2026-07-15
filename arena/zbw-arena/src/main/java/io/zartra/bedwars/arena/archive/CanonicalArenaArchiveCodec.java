package io.zartra.bedwars.arena.archive;

import io.zartra.bedwars.api.identity.ArenaId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.GeneratorTypeId;
import io.zartra.bedwars.api.identity.MapId;
import io.zartra.bedwars.api.identity.ResourceId;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.arena.application.ArenaFailures;
import io.zartra.bedwars.arena.model.ArenaBundle;
import io.zartra.bedwars.arena.model.ArenaDefinition;
import io.zartra.bedwars.arena.model.ArenaGenerator;
import io.zartra.bedwars.arena.model.ArenaHologram;
import io.zartra.bedwars.arena.model.ArenaLocation;
import io.zartra.bedwars.arena.model.ArenaNpc;
import io.zartra.bedwars.arena.model.ArenaRegion;
import io.zartra.bedwars.arena.model.ArenaTeam;
import io.zartra.bedwars.arena.model.MapDefinition;
import io.zartra.bedwars.world.api.WorldKey;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Canonical schema-one binary codec with explicit bounds and no Java object deserialization. */
public final class CanonicalArenaArchiveCodec implements ArenaArchiveCodec {
    private static final int MAGIC = 0x5a425741;
    private static final int SCHEMA = 1;

    @Override public Result<ArenaArchive> encode(final DefinitionId archiveId,
                                                 final ArenaBundle bundle,
                                                 final Instant createdAt) {
        if (archiveId == null || bundle == null || createdAt == null) {
            throw new NullPointerException("archive encode arguments must not be null");
        }
        try {
            final ByteArrayOutputStream bytes = new ByteArrayOutputStream(8192);
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeInt(MAGIC);
                output.writeInt(SCHEMA);
                writeArena(output, bundle.arena());
                writeMap(output, bundle.map());
            }
            return Result.success(ArenaArchive.create(archiveId, bundle.arenaId(), bundle.mapId(),
                    SCHEMA, createdAt, bytes.toByteArray()));
        } catch (IOException exception) {
            return Result.failure(ArenaFailures.ARCHIVE);
        }
    }

    @Override public Result<ArenaBundle> decode(final ArenaArchive archive) {
        if (archive == null) { throw new NullPointerException("archive"); }
        if (archive.schemaVersion() != SCHEMA) { return Result.failure(ArenaFailures.ARCHIVE); }
        try (DataInputStream input = new DataInputStream(
                new ByteArrayInputStream(archive.payload()))) {
            if (input.readInt() != MAGIC || input.readInt() != SCHEMA) {
                return Result.failure(ArenaFailures.ARCHIVE);
            }
            final ArenaDefinition arena = readArena(input);
            final MapDefinition map = readMap(input);
            if (input.read() != -1 || !arena.id().equals(archive.arenaId())
                    || !map.id().equals(archive.mapId())) {
                return Result.failure(ArenaFailures.ARCHIVE);
            }
            return Result.success(new ArenaBundle(arena, map));
        } catch (IOException | RuntimeException exception) {
            return Result.failure(ArenaFailures.ARCHIVE);
        }
    }

    private static void writeArena(final DataOutputStream output,
                                   final ArenaDefinition arena) throws IOException {
        output.writeUTF(arena.id().toString());
        output.writeUTF(arena.mapId().toString());
        output.writeUTF(arena.displayName());
        writeOptionalWorld(output, arena.world().orElse(null));
        writeOptionalWorld(output, arena.templateWorld().orElse(null));
        output.writeUTF(arena.worldAdapter().toString());
        output.writeUTF(arena.group().toString());
        writeDefinitions(output, arena.modes());
        output.writeInt(arena.minimumPlayers());
        output.writeInt(arena.maximumPlayers());
        output.writeInt(arena.teamSize());
        output.writeInt(arena.priority());
        output.writeInt(arena.rotationWeight());
        writeOptionalLocation(output, arena.waitingSpawn().orElse(null));
        writeOptionalLocation(output, arena.spectatorSpawn().orElse(null));
        writeOptionalRegion(output, arena.bounds().orElse(null));
        output.writeDouble(arena.voidY());
        output.writeDouble(arena.buildMinimumY());
        output.writeDouble(arena.buildMaximumY());
        output.writeInt(arena.teams().size());
        for (ArenaTeam team : arena.teams()) { writeTeam(output, team); }
        output.writeInt(arena.generators().size());
        for (ArenaGenerator generator : arena.generators()) { writeGenerator(output, generator); }
        output.writeInt(arena.npcs().size());
        for (ArenaNpc npc : arena.npcs()) { writeNpc(output, npc); }
        output.writeInt(arena.protectedRegions().size());
        for (ArenaRegion region : arena.protectedRegions()) { writeRegion(output, region); }
        output.writeInt(arena.holograms().size());
        for (ArenaHologram hologram : arena.holograms()) { writeHologram(output, hologram); }
        output.writeInt(arena.speeds().size());
        for (Map.Entry<DefinitionId, Duration> entry : arena.speeds().entrySet()) {
            output.writeUTF(entry.getKey().toString());
            output.writeLong(entry.getValue().toMillis());
        }
        writeDefinitions(output, arena.rules());
        writeMetadata(output, arena.metadata());
        output.writeUTF(arena.status().name());
        output.writeLong(arena.createdAt().toEpochMilli());
        output.writeLong(arena.updatedAt().toEpochMilli());
        output.writeLong(arena.version());
    }

    private static ArenaDefinition readArena(final DataInputStream input) throws IOException {
        final ArenaId id = ArenaId.parse(input.readUTF());
        final MapId mapId = MapId.parse(input.readUTF());
        final String name = input.readUTF();
        final WorldKey world = readOptionalWorld(input);
        final WorldKey template = readOptionalWorld(input);
        final DefinitionId adapter = DefinitionId.parse(input.readUTF());
        final DefinitionId group = DefinitionId.parse(input.readUTF());
        final Set<DefinitionId> modes = readDefinitions(input, 64);
        final int minimumPlayers = input.readInt();
        final int maximumPlayers = input.readInt();
        final int teamSize = input.readInt();
        final int priority = input.readInt();
        final int weight = input.readInt();
        final ArenaLocation waiting = readOptionalLocation(input);
        final ArenaLocation spectator = readOptionalLocation(input);
        final ArenaRegion bounds = readOptionalRegion(input);
        final double voidY = input.readDouble();
        final double buildMinimum = input.readDouble();
        final double buildMaximum = input.readDouble();
        final List<ArenaTeam> teams = new ArrayList<ArenaTeam>();
        for (int count = readCount(input, 64); count > 0; count--) { teams.add(readTeam(input)); }
        final List<ArenaGenerator> generators = new ArrayList<ArenaGenerator>();
        for (int count = readCount(input, 256); count > 0; count--) { generators.add(readGenerator(input)); }
        final List<ArenaNpc> npcs = new ArrayList<ArenaNpc>();
        for (int count = readCount(input, 256); count > 0; count--) { npcs.add(readNpc(input)); }
        final List<ArenaRegion> regions = new ArrayList<ArenaRegion>();
        for (int count = readCount(input, 128); count > 0; count--) { regions.add(readRegion(input)); }
        final List<ArenaHologram> holograms = new ArrayList<ArenaHologram>();
        for (int count = readCount(input, 128); count > 0; count--) { holograms.add(readHologram(input)); }
        final Map<DefinitionId, Duration> speeds = new TreeMap<DefinitionId, Duration>();
        for (int count = readCount(input, 64); count > 0; count--) {
            speeds.put(DefinitionId.parse(input.readUTF()), Duration.ofMillis(input.readLong()));
        }
        final Set<DefinitionId> rules = readDefinitions(input, 128);
        final Map<DefinitionId, String> metadata = readMetadata(input, 128);
        final ArenaDefinition.Status status = ArenaDefinition.Status.valueOf(input.readUTF());
        final Instant createdAt = Instant.ofEpochMilli(input.readLong());
        final Instant updatedAt = Instant.ofEpochMilli(input.readLong());
        final long version = input.readLong();
        return ArenaDefinition.builder(id, mapId, name, createdAt).worlds(world, template)
                .worldAdapter(adapter).group(group).modes(modes)
                .playerLimits(minimumPlayers, maximumPlayers, teamSize)
                .selection(priority, weight).waitingSpawn(waiting).spectatorSpawn(spectator)
                .bounds(bounds).limits(voidY, buildMinimum, buildMaximum).teams(teams)
                .generators(generators).npcs(npcs).protectedRegions(regions)
                .holograms(holograms).speeds(speeds).rules(rules).metadata(metadata)
                .status(status).revision(version, updatedAt).build();
    }

    private static void writeMap(final DataOutputStream output,
                                 final MapDefinition map) throws IOException {
        output.writeUTF(map.id().toString());
        output.writeUTF(map.displayName());
        output.writeLong(map.createdAt().toEpochMilli());
        output.writeLong(map.updatedAt().toEpochMilli());
        output.writeLong(map.version());
        output.writeUTF(map.template().toString());
        output.writeUTF(map.group().toString());
        output.writeUTF(map.author().toString());
        output.writeUTF(map.description());
        writeDefinitions(output, map.supportedModes());
        output.writeInt(map.minimumTeamSize());
        output.writeInt(map.maximumTeamSize());
        writeDefinitions(output, map.tags());
        writeMetadata(output, map.metadata());
        output.writeUTF(map.validationStatus().toString());
    }

    private static MapDefinition readMap(final DataInputStream input) throws IOException {
        return new MapDefinition(MapId.parse(input.readUTF()), input.readUTF(),
                Instant.ofEpochMilli(input.readLong()), Instant.ofEpochMilli(input.readLong()),
                input.readLong(), DefinitionId.parse(input.readUTF()),
                DefinitionId.parse(input.readUTF()), DefinitionId.parse(input.readUTF()),
                input.readUTF(), readDefinitions(input, 64), input.readInt(), input.readInt(),
                readDefinitions(input, 64), readMetadata(input, 128),
                DefinitionId.parse(input.readUTF()));
    }

    private static void writeTeam(final DataOutputStream output,
                                  final ArenaTeam team) throws IOException {
        output.writeUTF(team.id().toString());
        output.writeUTF(team.displayName());
        output.writeUTF(team.color().toString());
        writeOptionalLocation(output, team.spawn().orElse(null));
        writeOptionalLocation(output, team.bed().orElse(null));
        output.writeBoolean(team.bedFacing().isPresent());
        if (team.bedFacing().isPresent()) { output.writeUTF(team.bedFacing().get().toString()); }
    }

    private static ArenaTeam readTeam(final DataInputStream input) throws IOException {
        ArenaTeam team = ArenaTeam.create(DefinitionId.parse(input.readUTF()), input.readUTF(),
                DefinitionId.parse(input.readUTF()));
        final ArenaLocation spawn = readOptionalLocation(input);
        final ArenaLocation bed = readOptionalLocation(input);
        final DefinitionId facing = input.readBoolean() ? DefinitionId.parse(input.readUTF()) : null;
        if (spawn != null) { team = team.withSpawn(spawn); }
        if (bed != null) { team = team.withBed(bed, facing); }
        return team;
    }

    private static void writeGenerator(final DataOutputStream output,
                                       final ArenaGenerator value) throws IOException {
        output.writeUTF(value.id().toString());
        output.writeUTF(value.type().toString());
        output.writeUTF(value.resource().toString());
        output.writeBoolean(value.teamId().isPresent());
        if (value.teamId().isPresent()) { output.writeUTF(value.teamId().get().toString()); }
        writeLocation(output, value.location());
        output.writeLong(value.interval().toMillis());
    }

    private static ArenaGenerator readGenerator(final DataInputStream input) throws IOException {
        final DefinitionId id = DefinitionId.parse(input.readUTF());
        final GeneratorTypeId type = GeneratorTypeId.parse(input.readUTF());
        final ResourceId resource = ResourceId.parse(input.readUTF());
        final DefinitionId team = input.readBoolean() ? DefinitionId.parse(input.readUTF()) : null;
        return ArenaGenerator.of(id, type, resource, team, readLocation(input),
                Duration.ofMillis(input.readLong()));
    }

    private static void writeNpc(final DataOutputStream output,
                                 final ArenaNpc value) throws IOException {
        output.writeUTF(value.id().toString());
        output.writeUTF(value.kind().name());
        output.writeBoolean(value.teamId().isPresent());
        if (value.teamId().isPresent()) { output.writeUTF(value.teamId().get().toString()); }
        writeLocation(output, value.location());
    }

    private static ArenaNpc readNpc(final DataInputStream input) throws IOException {
        final DefinitionId id = DefinitionId.parse(input.readUTF());
        final ArenaNpc.Kind kind = ArenaNpc.Kind.valueOf(input.readUTF());
        final DefinitionId team = input.readBoolean() ? DefinitionId.parse(input.readUTF()) : null;
        return ArenaNpc.of(id, kind, team, readLocation(input));
    }

    private static void writeHologram(final DataOutputStream output,
                                      final ArenaHologram value) throws IOException {
        output.writeUTF(value.id().toString());
        writeLocation(output, value.location());
        output.writeInt(value.messageKeys().size());
        for (DefinitionId message : value.messageKeys()) { output.writeUTF(message.toString()); }
    }

    private static ArenaHologram readHologram(final DataInputStream input) throws IOException {
        final DefinitionId id = DefinitionId.parse(input.readUTF());
        final ArenaLocation location = readLocation(input);
        final List<DefinitionId> messages = new ArrayList<DefinitionId>();
        for (int count = readCount(input, 16); count > 0; count--) {
            messages.add(DefinitionId.parse(input.readUTF()));
        }
        return new ArenaHologram(id, location, messages);
    }

    private static void writeOptionalWorld(final DataOutputStream output,
                                           final WorldKey value) throws IOException {
        output.writeBoolean(value != null);
        if (value != null) { output.writeUTF(value.value()); }
    }
    private static WorldKey readOptionalWorld(final DataInputStream input) throws IOException {
        return input.readBoolean() ? WorldKey.of(input.readUTF()) : null;
    }
    private static void writeOptionalLocation(final DataOutputStream output,
                                              final ArenaLocation value) throws IOException {
        output.writeBoolean(value != null);
        if (value != null) { writeLocation(output, value); }
    }
    private static ArenaLocation readOptionalLocation(final DataInputStream input) throws IOException {
        return input.readBoolean() ? readLocation(input) : null;
    }
    private static void writeLocation(final DataOutputStream output,
                                      final ArenaLocation value) throws IOException {
        output.writeDouble(value.x());
        output.writeDouble(value.y());
        output.writeDouble(value.z());
        output.writeFloat(value.yaw());
        output.writeFloat(value.pitch());
    }
    private static ArenaLocation readLocation(final DataInputStream input) throws IOException {
        return ArenaLocation.of(input.readDouble(), input.readDouble(), input.readDouble(),
                input.readFloat(), input.readFloat());
    }
    private static void writeOptionalRegion(final DataOutputStream output,
                                            final ArenaRegion value) throws IOException {
        output.writeBoolean(value != null);
        if (value != null) { writeRegion(output, value); }
    }
    private static ArenaRegion readOptionalRegion(final DataInputStream input) throws IOException {
        return input.readBoolean() ? readRegion(input) : null;
    }
    private static void writeRegion(final DataOutputStream output,
                                    final ArenaRegion value) throws IOException {
        output.writeUTF(value.id().toString());
        writeLocation(output, value.minimum());
        writeLocation(output, value.maximum());
    }
    private static ArenaRegion readRegion(final DataInputStream input) throws IOException {
        return ArenaRegion.between(DefinitionId.parse(input.readUTF()), readLocation(input),
                readLocation(input));
    }
    private static void writeDefinitions(final DataOutputStream output,
                                         final Set<DefinitionId> values) throws IOException {
        output.writeInt(values.size());
        for (DefinitionId value : values) { output.writeUTF(value.toString()); }
    }
    private static Set<DefinitionId> readDefinitions(final DataInputStream input,
                                                     final int maximum) throws IOException {
        final Set<DefinitionId> values = new TreeSet<DefinitionId>();
        final int count = readCount(input, maximum);
        for (int index = 0; index < count; index++) {
            if (!values.add(DefinitionId.parse(input.readUTF()))) {
                throw new IOException("duplicate definition identity");
            }
        }
        return values;
    }
    private static void writeMetadata(final DataOutputStream output,
                                      final Map<DefinitionId, String> values) throws IOException {
        output.writeInt(values.size());
        for (Map.Entry<DefinitionId, String> entry : values.entrySet()) {
            output.writeUTF(entry.getKey().toString());
            output.writeUTF(entry.getValue());
        }
    }
    private static Map<DefinitionId, String> readMetadata(final DataInputStream input,
                                                          final int maximum) throws IOException {
        final Map<DefinitionId, String> values = new TreeMap<DefinitionId, String>();
        final int count = readCount(input, maximum);
        for (int index = 0; index < count; index++) {
            if (values.put(DefinitionId.parse(input.readUTF()), input.readUTF()) != null) {
                throw new IOException("duplicate metadata identity");
            }
        }
        return values;
    }
    private static int readCount(final DataInputStream input, final int maximum) throws IOException {
        final int value;
        try { value = input.readInt(); }
        catch (EOFException exception) { throw new IOException("truncated archive", exception); }
        if (value < 0 || value > maximum) { throw new IOException("archive collection bound exceeded"); }
        return value;
    }
}
