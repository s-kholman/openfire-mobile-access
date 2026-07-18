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

    public void ensureSchema() {
        try (Connection connection = DbConnectionManager.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(CREATE_CREDENTIAL_TABLE);
        } catch (final SQLException e) {
            throw new IllegalStateException("Unable to initialize Mobile Access database schema", e);
        }
    }
}
