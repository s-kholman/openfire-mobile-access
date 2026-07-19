package ru.krimm.openfire.mobileaccess.directory;

import java.util.Locale;
import java.util.Optional;

import org.jivesoftware.openfire.user.User;
import org.jivesoftware.openfire.user.UserManager;
import org.jivesoftware.openfire.user.UserNotFoundException;

/**
 * Directory adapter that delegates to the UserProvider configured in Openfire.
 *
 * <p>With LDAPUserProvider configured, successful lookup means the LDAP entry passed
 * {@code ldap.searchFilter}. That filter is the authoritative access rule and can include
 * Active Directory nested-group matching without depending on GroupProvider visibility.</p>
 */
public final class OpenfireDirectoryGateway implements DirectoryGateway {

    private final UserManager userManager;

    public OpenfireDirectoryGateway() {
        this(UserManager.getInstance());
    }

    OpenfireDirectoryGateway(final UserManager userManager) {
        this.userManager = userManager;
    }

    @Override
    public Optional<DirectoryUser> findUser(final String username) {
        final String normalizedUsername = normalizeUsername(username);
        try {
            final User user = userManager.getUser(normalizedUsername);
            return Optional.of(new DirectoryUser(user.getUsername(), user.getName(), user.getEmail()));
        } catch (final UserNotFoundException exception) {
            return Optional.empty();
        }
    }

    private static String normalizeUsername(final String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        return username.trim().toLowerCase(Locale.ROOT);
    }
}
