package ru.krimm.openfire.mobileaccess.auth;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import org.jivesoftware.openfire.auth.AuthProvider;
import org.jivesoftware.openfire.auth.ConnectionException;
import org.jivesoftware.openfire.auth.InternalUnauthenticatedException;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.user.UserManager;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Authentication provider for locally managed mobile credentials. */
public final class MobileAccessAuthProvider implements AuthProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(MobileAccessAuthProvider.class);

    private final JdbcCredentialStore credentialStore = new JdbcCredentialStore();
    private final Pbkdf2CredentialVerifier verifier = new Pbkdf2CredentialVerifier();
    private final DirectoryUserLookup directoryUserLookup;

    public MobileAccessAuthProvider() {
        this(username -> UserManager.getInstance().getUser(username));
    }

    MobileAccessAuthProvider(final DirectoryUserLookup directoryUserLookup) {
        this.directoryUserLookup = Objects.requireNonNull(directoryUserLookup, "directoryUserLookup must not be null");
    }

    @Override
    public void authenticate(final String username, final String password)
            throws UnauthorizedException, ConnectionException, InternalUnauthenticatedException {
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            throw new UnauthorizedException("Invalid username or password");
        }

        final String normalizedUsername = normalizeUsername(username);
        final char[] passwordCharacters = password.toCharArray();
        try {
            requireDirectoryEligibility(normalizedUsername);
            final StoredCredential credential = credentialStore.find(normalizedUsername)
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));

            if (!verifier.verify(passwordCharacters, credential)) {
                throw new UnauthorizedException("Invalid username or password");
            }
        } catch (final SQLException e) {
            throw new ConnectionException("Unable to access the Mobile Access credential store", e);
        } finally {
            Arrays.fill(passwordCharacters, '\0');
        }
    }

    void requireDirectoryEligibility(final String username) throws UnauthorizedException {
        try {
            directoryUserLookup.requireExisting(username);
            LOGGER.debug(
                "Mobile Access directory user '{}' validated by configured UserProvider",
                username
            );
        } catch (final UserNotFoundException e) {
            LOGGER.info(
                "Mobile Access authentication rejected: directory user '{}' was not found by configured UserProvider",
                username
            );
            throw new UnauthorizedException("Invalid username or password");
        }
    }

    static String normalizeUsername(final String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username must not be blank");
        }
        String value = username.trim();
        final int backslash = value.lastIndexOf('\\');
        if (backslash >= 0) {
            value = value.substring(backslash + 1);
        }
        final int at = value.indexOf('@');
        if (at >= 0) {
            value = value.substring(0, at);
        }
        value = value.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Username must contain an account name");
        }
        return value.toLowerCase(Locale.ROOT);
    }

    @Override
    public String getPassword(final String username) throws UserNotFoundException {
        throw new UnsupportedOperationException("Mobile passwords are never retrievable");
    }

    @Override
    public void setPassword(final String username, final String password) throws UserNotFoundException {
        throw new UnsupportedOperationException("Passwords are managed by the Mobile Access plugin");
    }

    @Override
    public boolean supportsPasswordRetrieval() {
        return false;
    }

    @Override
    public boolean isScramSupported() {
        return false;
    }

    @Override
    public String getSalt(final String username) throws UserNotFoundException {
        throw new UnsupportedOperationException("SCRAM is not supported");
    }

    @Override
    public int getIterations(final String username) throws UserNotFoundException {
        throw new UnsupportedOperationException("SCRAM is not supported");
    }

    @Override
    public String getServerKey(final String username) throws UserNotFoundException {
        throw new UnsupportedOperationException("SCRAM is not supported");
    }

    @Override
    public String getStoredKey(final String username) throws UserNotFoundException {
        throw new UnsupportedOperationException("SCRAM is not supported");
    }

    @FunctionalInterface
    interface DirectoryUserLookup {
        void requireExisting(String username) throws UserNotFoundException;
    }
}
