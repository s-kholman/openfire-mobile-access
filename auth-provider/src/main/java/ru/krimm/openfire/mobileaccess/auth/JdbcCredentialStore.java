package ru.krimm.openfire.mobileaccess.auth;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import org.jivesoftware.database.DbConnectionManager;

final class JdbcCredentialStore {

    Optional<StoredCredential> find(final String username) throws SQLException {
        final String sql = "SELECT algorithm, iterations, salt, passwordHash, enabled FROM ofMobileAccessCredential WHERE username = ?";
        try (Connection connection = DbConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                return Optional.of(new StoredCredential(
                    result.getString("algorithm"),
                    result.getInt("iterations"),
                    result.getString("salt"),
                    result.getString("passwordHash"),
                    result.getBoolean("enabled")
                ));
            }
        }
    }
}
