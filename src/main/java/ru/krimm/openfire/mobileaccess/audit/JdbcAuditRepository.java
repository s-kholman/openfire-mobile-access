package ru.krimm.openfire.mobileaccess.audit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import org.jivesoftware.database.DbConnectionManager;

/** Persists security-relevant Mobile Access administration events. */
public final class JdbcAuditRepository {

    private static final String INSERT = """
        INSERT INTO ofMobileAccessAudit (eventAt, actor, action, targetUsername, outcome, details)
        VALUES (?, ?, ?, ?, ?, ?)
        """;

    public void record(
        final Instant eventAt,
        final String actor,
        final AuditAction action,
        final String targetUsername,
        final AuditOutcome outcome,
        final String details
    ) {
        Objects.requireNonNull(eventAt);
        Objects.requireNonNull(action);
        Objects.requireNonNull(outcome);

        try (Connection connection = DbConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT)) {
            statement.setLong(1, eventAt.toEpochMilli());
            statement.setString(2, normalize(actor));
            statement.setString(3, action.name());
            statement.setString(4, normalize(targetUsername));
            statement.setString(5, outcome.name());
            statement.setString(6, truncate(details, 1000));
            statement.executeUpdate();
        } catch (final SQLException e) {
            throw new IllegalStateException("Unable to write Mobile Access audit event", e);
        }
    }

    private static String normalize(final String value) {
        return value == null || value.isBlank() ? "unknown" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String truncate(final String value, final int maximumLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maximumLength ? value : value.substring(0, maximumLength);
    }
}
