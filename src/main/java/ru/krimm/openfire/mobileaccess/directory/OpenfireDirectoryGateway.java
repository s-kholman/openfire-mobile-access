package ru.krimm.openfire.mobileaccess.directory;

import java.util.Optional;

import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.group.GroupManager;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.openfire.user.UserManager;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.xmpp.packet.JID;

/**
 * Directory adapter that delegates to the user and group providers configured in Openfire.
 *
 * <p>When LDAP providers are configured, these calls resolve users and groups from LDAP
 * through the regular Openfire manager APIs. No direct LDAP connection is created by this plugin.</p>
 */
public final class OpenfireDirectoryGateway implements DirectoryGateway {

    private final UserManager userManager;
    private final GroupManager groupManager;
    private final XMPPServer xmppServer;

    public OpenfireDirectoryGateway() {
        this(UserManager.getInstance(), GroupManager.getInstance(), XMPPServer.getInstance());
    }

    OpenfireDirectoryGateway(
        final UserManager userManager,
        final GroupManager groupManager,
        final XMPPServer xmppServer
    ) {
        this.userManager = userManager;
        this.groupManager = groupManager;
        this.xmppServer = xmppServer;
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

    @Override
    public boolean isMemberOfConfiguredGroupSet(final String username) {
        final String normalizedUsername = normalizeUsername(username);
        final JID userJid = xmppServer.createJID(normalizedUsername, null).asBareJID();
        return !groupManager.getGroups(userJid).isEmpty();
    }

    private static String normalizeUsername(final String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }

        String result = username.trim();
        final int slash = result.lastIndexOf('\\');
        if (slash >= 0) {
            result = result.substring(slash + 1);
        }
        final int at = result.indexOf('@');
        if (at >= 0) {
            result = result.substring(0, at);
        }
        if (result.isBlank()) {
            throw new IllegalArgumentException("username must contain an Openfire localpart");
        }
        return result;
    }
}
