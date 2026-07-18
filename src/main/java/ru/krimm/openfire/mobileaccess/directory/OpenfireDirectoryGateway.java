package ru.krimm.openfire.mobileaccess.directory;

import java.util.Locale;
import java.util.Optional;

import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.group.Group;
import org.jivesoftware.openfire.group.GroupManager;
import org.jivesoftware.openfire.group.GroupNotFoundException;
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
    public boolean groupExists(final String groupName) {
        try {
            groupManager.getGroup(requireGroupName(groupName));
            return true;
        } catch (final GroupNotFoundException exception) {
            return false;
        }
    }

    @Override
    public boolean isMemberOf(final String username, final String groupName) {
        final String normalizedUsername = normalizeUsername(username);
        try {
            final Group group = groupManager.getGroup(requireGroupName(groupName));
            final JID userJid = xmppServer.createJID(normalizedUsername, null).asBareJID();
            return group.getAll().contains(userJid);
        } catch (final GroupNotFoundException exception) {
            return false;
        }
    }

    private static String normalizeUsername(final String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private static String requireGroupName(final String groupName) {
        if (groupName == null || groupName.isBlank()) {
            throw new IllegalArgumentException("groupName must not be blank");
        }
        return groupName.trim();
    }
}
