package ru.krimm.openfire.mobileaccess.credential;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import org.jivesoftware.database.DbConnectionManager;

public final class JdbcMobileCredentialRepository implements MobileCredentialRepository {

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

    @Override
    public void save(final String username, final Pbkdf2PasswordHasher.HashedPassword password, final Instant changedAt) {
        try (Connection connection = DbConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPSERT)) {
            statement.setString(1, username);
            statement.setString(2, password.algorithm());
            statement.setInt(3, password.iterations());
            statement.setString(4, password.salt());
            statement.setString(5, password.hash());
            statement.setLong(6, changedAt.toEpochMilli());
            statement.setLong(7, changedAt.toEpochMilli());
            statement.executeUpdate();
        } catch (final SQLException e) {
            throw new IllegalStateException("Unable to save mobile credential", e);
        }
    }

    @Override
    public void revoke(final String username, final Instant revokedAt) {
        final String sql = "UPDATE ofMobileAccessCredential SET enabled = FALSE, revokedAt = ?, updatedAt = ? WHERE username = ?";
        try (Connection connection = DbConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, revokedAt.toEpochMilli());
            statement.setLong(2, revokedAt.toEpochMilli());
            statement.setString(3, username);
            statement.executeUpdate();
        } catch (final SQLException e) {
            throw new IllegalStateException("Unable to revoke mobile credential", e);
        }
    }

    @Override
    public Optional<MobileCredentialRecord> find(final String username) {
        final String sql = "SELECT username, algorithm, iterations, salt, passwordHash, enabled, updatedAt, revokedAt FROM ofMobileAccessCredential WHERE username = ?";
        try (Connection connection = DbConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                final long revokedAtValue = result.getLong("revokedAt");
                final Instant revokedAt = result.wasNull() ? null : Instant.ofEpochMilli(revokedAtValue);
                return Optional.of(new MobileCredentialRecord(
                    result.getString("username"), result.getString("algorithm"), result.getInt("iterations"),
                    result.getString("salt"), result.getString("passwordHash"), result.getBoolean("enabled"),
                    Instant.ofEpochMilli(result.getLong("updatedAt")), revokedAt
                ));
            }
        } catch (final SQLException e) {
            throw new IllegalStateException("Unable to read mobile credential", e);
        }
    }
}
