package ru.krimm.openfire.mobileaccess.directory;

import java.util.Optional;

/**
 * Boundary between plugin business logic and Openfire user/group providers.
 */
public interface DirectoryGateway {

    Optional<DirectoryUser> findUser(String username);

    boolean groupExists(String groupName);

    boolean isMemberOf(String username, String groupName);
}
