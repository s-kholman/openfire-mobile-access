package ru.krimm.openfire.mobileaccess.credential;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jivesoftware.database.DbConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public final class JdbcMobileCredentialRepository implements MobileCredentialRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcMobileCredentialRepository.class);

    private static final String UPSERT = """
        INSERT INTO ofMobileAccessCredential
            (username, algorithm, iterations, salt, passwordHash, enabled, createdAt, updatedAt, revokedAt)
        VALUES (?, ?, ?, ?, ?, TRUE, ?, ?, NULL)
        ON CONFLICT (username) DO UPDATE SET
            algorithm = EXCLUDED.algorithm,
            iterations = EXCLUDED.iterations,
            salt = EXCLUDED.salt,
            passwordHash = EXCLUDED.passwordHash,
            enabled = TRUE,
            updatedAt = EXCLUDED.updatedAt,
            revokedAt = NULL
        """;

    private static final String SELECT_COLUMNS =
        "username, algorithm, iterations, salt, passwordHash, enabled, updatedAt, revokedAt";

    @Override
    public void save(final String username, final Pbkdf2PasswordHasher.HashedPassword password, final Instant changedAt) {
        LOGGER.info("[RID:{}] SQL UPSERT started: table=ofMobileAccessCredential, username={}, algorithm={}, iterations={}", rid(), username, password.algorithm(), password.iterations());
        try (Connection connection = DbConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPSERT)) {
            statement.setString(1, username);
            statement.setString(2, password.algorithm());
            statement.setInt(3, password.iterations());
            statement.setString(4, password.salt());
            statement.setString(5, password.hash());
            statement.setLong(6, changedAt.toEpochMilli());
            statement.setLong(7, changedAt.toEpochMilli());
            final int affectedRows = statement.executeUpdate();
            LOGGER.info("[RID:{}] SQL UPSERT completed: username={}, affectedRows={}", rid(), username, affectedRows);
            if (affectedRows < 1) {
                throw new IllegalStateException("Unable to save mobile credential: SQL affected zero rows");
            }
        } catch (final SQLException e) {
            LOGGER.error("[RID:{}] SQL UPSERT failed: username={}, sqlState={}, errorCode={}", rid(), username, e.getSQLState(), e.getErrorCode(), e);
            throw new IllegalStateException("Unable to save mobile credential", e);
        }
    }

    @Override
    public void revoke(final String username, final Instant revokedAt) {
        executeUpdate(
            "UPDATE ofMobileAccessCredential SET enabled = FALSE, revokedAt = ?, updatedAt = ? WHERE username = ?",
            username,
            revokedAt
        );
    }

    @Override
    public void enable(final String username, final Instant changedAt) {
        executeUpdate(
            "UPDATE ofMobileAccessCredential SET enabled = TRUE, revokedAt = NULL, updatedAt = ? WHERE username = ?",
            username,
            changedAt
        );
    }

    @Override
    public void delete(final String username) {
        LOGGER.info("[RID:{}] SQL DELETE started: username={}", rid(), username);
        try (Connection connection = DbConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "DELETE FROM ofMobileAccessCredential WHERE username = ?")) {
            statement.setString(1, username);
            final int affectedRows = statement.executeUpdate();
            LOGGER.info("[RID:{}] SQL DELETE completed: username={}, affectedRows={}", rid(), username, affectedRows);
        } catch (final SQLException e) {
            LOGGER.error("[RID:{}] SQL DELETE failed: username={}, sqlState={}, errorCode={}", rid(), username, e.getSQLState(), e.getErrorCode(), e);
            throw new IllegalStateException("Unable to delete mobile credential", e);
        }
    }

    @Override
    public Optional<MobileCredentialRecord> find(final String username) {
        final String sql = "SELECT " + SELECT_COLUMNS + " FROM ofMobileAccessCredential WHERE username = ?";
        try (Connection connection = DbConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        } catch (final SQLException e) {
            LOGGER.error("[RID:{}] SQL SELECT failed: username={}, sqlState={}, errorCode={}", rid(), username, e.getSQLState(), e.getErrorCode(), e);
            throw new IllegalStateException("Unable to read mobile credential", e);
        }
    }

    @Override
    public List<MobileCredentialRecord> findAll() {
        final String sql = "SELECT " + SELECT_COLUMNS + " FROM ofMobileAccessCredential ORDER BY username";
        final List<MobileCredentialRecord> records = new ArrayList<>();
        try (Connection connection = DbConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                records.add(map(result));
            }
            LOGGER.info("[RID:{}] SQL list completed: count={}", rid(), records.size());
            return records;
        } catch (final SQLException e) {
            LOGGER.error("[RID:{}] SQL list failed: sqlState={}, errorCode={}", rid(), e.getSQLState(), e.getErrorCode(), e);
            throw new IllegalStateException("Unable to list mobile credentials", e);
        }
    }

    private static void executeUpdate(final String sql, final String username, final Instant changedAt) {
        LOGGER.info("[RID:{}] SQL state update started: username={}", rid(), username);
        try (Connection connection = DbConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (sql.contains("revokedAt = ?")) {
                statement.setLong(1, changedAt.toEpochMilli());
                statement.setLong(2, changedAt.toEpochMilli());
                statement.setString(3, username);
            } else {
                statement.setLong(1, changedAt.toEpochMilli());
                statement.setString(2, username);
            }
            final int affectedRows = statement.executeUpdate();
            LOGGER.info("[RID:{}] SQL state update completed: username={}, affectedRows={}", rid(), username, affectedRows);
            if (affectedRows < 1) {
                throw new IllegalStateException("Unable to update mobile credential state: SQL affected zero rows");
            }
        } catch (final SQLException e) {
            LOGGER.error("[RID:{}] SQL state update failed: username={}, sqlState={}, errorCode={}", rid(), username, e.getSQLState(), e.getErrorCode(), e);
            throw new IllegalStateException("Unable to update mobile credential state", e);
        }
    }

    private static MobileCredentialRecord map(final ResultSet result) throws SQLException {
        final long revokedAtValue = result.getLong("revokedAt");
        final Instant revokedAt = result.wasNull() ? null : Instant.ofEpochMilli(revokedAtValue);
        return new MobileCredentialRecord(
            result.getString("username"),
            result.getString("algorithm"),
            result.getInt("iterations"),
            result.getString("salt"),
            result.getString("passwordHash"),
            result.getBoolean("enabled"),
            Instant.ofEpochMilli(result.getLong("updatedAt")),
            revokedAt
        );
    }

    private static String rid() {
        final String value = MDC.get("mobileAccessRequestId");
        return value == null ? "none" : value;
    }
}
