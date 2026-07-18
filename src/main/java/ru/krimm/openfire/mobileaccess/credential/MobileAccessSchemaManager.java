package ru.krimm.openfire.mobileaccess.credential;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.jivesoftware.database.DbConnectionManager;

public final class MobileAccessSchemaManager {

    private static final String CREATE_CREDENTIAL_TABLE = """
        CREATE TABLE IF NOT EXISTS ofMobileAccessCredential (
            username VARCHAR(64) PRIMARY KEY,
            algorithm VARCHAR(64) NOT NULL,
            iterations INTEGER NOT NULL,
            salt VARCHAR(128) NOT NULL,
            passwordHash VARCHAR(256) NOT NULL,
            enabled BOOLEAN NOT NULL DEFAULT TRUE,
            createdAt BIGINT NOT NULL,
            updatedAt BIGINT NOT NULL,
            revokedAt BIGINT NULL
        )
        """;

    private static final String CREATE_AUDIT_TABLE = """
        CREATE TABLE IF NOT EXISTS ofMobileAccessAudit (
            id BIGSERIAL PRIMARY KEY,
            eventAt BIGINT NOT NULL,
            actor VARCHAR(128) NOT NULL,
            action VARCHAR(64) NOT NULL,
            targetUsername VARCHAR(64) NOT NULL,
            outcome VARCHAR(32) NOT NULL,
            details VARCHAR(1000) NULL
        )
        """;

    private static final String CREATE_AUDIT_INDEX = """
        CREATE INDEX IF NOT EXISTS ofMobileAccessAudit_eventAt_idx
        ON ofMobileAccessAudit (eventAt DESC)
        """;

    public void ensureSchema() {
        try (Connection connection = DbConnectionManager.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(CREATE_CREDENTIAL_TABLE);
            statement.execute(CREATE_AUDIT_TABLE);
            statement.execute(CREATE_AUDIT_INDEX);
        } catch (final SQLException e) {
            throw new IllegalStateException("Unable to initialize Mobile Access database schema", e);
        }
    }
}
