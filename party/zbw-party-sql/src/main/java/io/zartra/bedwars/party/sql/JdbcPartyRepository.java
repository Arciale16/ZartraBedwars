package io.zartra.bedwars.party.sql;

import io.zartra.bedwars.api.identity.PartyId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.identity.ProviderId;
import io.zartra.bedwars.party.Party;
import io.zartra.bedwars.party.PartyInvitation;
import io.zartra.bedwars.party.PartyPrivacy;
import io.zartra.bedwars.party.PartyRepository;
import io.zartra.bedwars.party.PartyState;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import javax.sql.DataSource;

/** Prepared-statement asynchronous native party repository. */
public final class JdbcPartyRepository implements PartyRepository {
    private final DataSource source;
    private final Executor executor;

    /**
     * Creates the repository.
     *
     * @param source JDBC source
     * @param executor bounded storage executor
     */
    public JdbcPartyRepository(final DataSource source, final Executor executor) {
        this.source = Objects.requireNonNull(source, "source");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    @Override
    public CompletionStage<Boolean> create(final Party party) {
        Objects.requireNonNull(party, "party");
        return transaction(connection -> {
            insertParty(connection, party);
            replaceMembers(connection, party);
            replaceInvitations(connection, party);
            return true;
        }, false);
    }

    @Override
    public CompletionStage<SaveResult> save(final Party party,
                                            final long expectedRevision) {
        Objects.requireNonNull(party, "party");
        if (expectedRevision < 0 || party.revision() <= expectedRevision) {
            throw new IllegalArgumentException("updated revision must exceed expected revision");
        }
        return transaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE parties SET state=?,leader_id=?,privacy=?,migration_target=?,revision=?"
                            + " WHERE party_id=? AND revision=?")) {
                bindPartyUpdate(statement, party, expectedRevision);
                if (statement.executeUpdate() != 1) {
                    return exists(connection, party.partyId())
                            ? SaveResult.CONFLICT : SaveResult.NOT_FOUND;
                }
            }
            deleteChildren(connection, party.partyId());
            replaceMembers(connection, party);
            replaceInvitations(connection, party);
            return SaveResult.UPDATED;
        }, SaveResult.CONFLICT);
    }

    @Override
    public CompletionStage<Optional<Party>> find(final PartyId partyId) {
        Objects.requireNonNull(partyId, "partyId");
        return transaction(connection -> load(connection, partyId),
                Optional.<Party>empty());
    }

    @Override
    public CompletionStage<Optional<Party>> findByMember(final PlayerId memberId) {
        Objects.requireNonNull(memberId, "memberId");
        return transaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT party_id FROM party_members WHERE member_id=?")) {
                statement.setString(1, memberId.toString());
                try (ResultSet rows = statement.executeQuery()) {
                    return rows.next()
                            ? load(connection, PartyId.parse(rows.getString(1)))
                            : Optional.empty();
                }
            }
        }, Optional.<Party>empty());
    }

    private static void insertParty(final Connection connection, final Party party)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO parties VALUES(?,?,?,?,?,?)")) {
            statement.setString(1, party.partyId().toString());
            statement.setString(2, party.state().name());
            statement.setString(3, party.leaderId().toString());
            statement.setString(4, party.privacy().name());
            statement.setString(5, party.migrationTarget().map(Object::toString).orElse(null));
            statement.setLong(6, party.revision());
            statement.executeUpdate();
        }
    }

    private static void bindPartyUpdate(final PreparedStatement statement,
                                        final Party party,
                                        final long expectedRevision) throws SQLException {
        statement.setString(1, party.state().name());
        statement.setString(2, party.leaderId().toString());
        statement.setString(3, party.privacy().name());
        statement.setString(4, party.migrationTarget().map(Object::toString).orElse(null));
        statement.setLong(5, party.revision());
        statement.setString(6, party.partyId().toString());
        statement.setLong(7, expectedRevision);
    }

    private static void replaceMembers(final Connection connection, final Party party)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO party_members VALUES(?,?,?)")) {
            int order = 0;
            for (PlayerId member : party.members()) {
                statement.setString(1, party.partyId().toString());
                statement.setString(2, member.toString());
                statement.setInt(3, order++);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static void replaceInvitations(final Connection connection, final Party party)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO party_invitations VALUES(?,?,?,?,?)")) {
            for (PartyInvitation invitation : party.invitations()) {
                statement.setString(1, party.partyId().toString());
                statement.setString(2, invitation.invitee().toString());
                statement.setString(3, invitation.invitedBy().toString());
                statement.setLong(4, invitation.createdAt().toEpochMilli());
                statement.setLong(5, invitation.expiresAt().toEpochMilli());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static void deleteChildren(final Connection connection,
                                       final PartyId partyId) throws SQLException {
        try (PreparedStatement invitations = connection.prepareStatement(
                "DELETE FROM party_invitations WHERE party_id=?");
             PreparedStatement members = connection.prepareStatement(
                     "DELETE FROM party_members WHERE party_id=?")) {
            invitations.setString(1, partyId.toString());
            invitations.executeUpdate();
            members.setString(1, partyId.toString());
            members.executeUpdate();
        }
    }

    private static boolean exists(final Connection connection, final PartyId partyId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM parties WHERE party_id=?")) {
            statement.setString(1, partyId.toString());
            try (ResultSet rows = statement.executeQuery()) { return rows.next(); }
        }
    }

    private static Optional<Party> load(final Connection connection,
                                        final PartyId partyId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM parties WHERE party_id=?")) {
            statement.setString(1, partyId.toString());
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) { return Optional.empty(); }
                String migration = rows.getString("migration_target");
                return Optional.of(new Party(
                        partyId,
                        parse(PartyState.class, rows.getString("state")),
                        PlayerId.parse(rows.getString("leader_id")),
                        loadMembers(connection, partyId),
                        loadInvitations(connection, partyId),
                        parse(PartyPrivacy.class, rows.getString("privacy")),
                        migration == null ? null : ProviderId.parse(migration),
                        rows.getLong("revision")));
            }
        }
    }

    private static List<PlayerId> loadMembers(final Connection connection,
                                              final PartyId partyId) throws SQLException {
        List<PlayerId> members = new ArrayList<PlayerId>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT member_id FROM party_members WHERE party_id=? ORDER BY member_order")) {
            statement.setString(1, partyId.toString());
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) { members.add(PlayerId.parse(rows.getString(1))); }
            }
        }
        return members;
    }

    private static List<PartyInvitation> loadInvitations(final Connection connection,
                                                         final PartyId partyId)
            throws SQLException {
        List<PartyInvitation> invitations = new ArrayList<PartyInvitation>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM party_invitations WHERE party_id=? ORDER BY created_at,invitee_id")) {
            statement.setString(1, partyId.toString());
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    invitations.add(new PartyInvitation(
                            PlayerId.parse(rows.getString("invited_by")),
                            PlayerId.parse(rows.getString("invitee_id")),
                            Instant.ofEpochMilli(rows.getLong("created_at")),
                            Instant.ofEpochMilli(rows.getLong("expires_at"))));
                }
            }
        }
        return invitations;
    }

    private <T> CompletionStage<T> transaction(final Work<T> work, final T duplicate) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = source.getConnection()) {
                connection.setAutoCommit(false);
                try {
                    T result = work.run(connection);
                    connection.commit();
                    return result;
                } catch (SQLException failure) {
                    rollback(connection);
                    if (isDuplicate(failure)) { return duplicate; }
                    throw new PartyPersistenceException("party transaction failed", failure);
                } catch (RuntimeException failure) {
                    rollback(connection);
                    throw new PartyPersistenceException(
                            "malformed party persistence data", failure);
                }
            } catch (SQLException failure) {
                throw new PartyPersistenceException("party connection failed", failure);
            }
        }, executor);
    }

    private static boolean isDuplicate(final SQLException failure) {
        String message = failure.getMessage();
        return message != null && (message.toLowerCase(Locale.ROOT).contains("unique")
                || message.toLowerCase(Locale.ROOT).contains("primary key"));
    }

    private static void rollback(final Connection connection) {
        try { connection.rollback(); } catch (SQLException ignored) { }
    }

    private static <E extends Enum<E>> E parse(final Class<E> type, final String value) {
        try { return Enum.valueOf(type, value); }
        catch (RuntimeException failure) {
            throw new PartyPersistenceException("malformed " + type.getSimpleName(), failure);
        }
    }

    private interface Work<T> {
        T run(Connection connection) throws SQLException;
    }
}
