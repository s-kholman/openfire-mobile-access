package ru.krimm.openfire.mobileaccess.directory;

import java.util.Optional;

/**
 * Boundary between plugin business logic and Openfire user/group providers.
 */
public interface DirectoryGateway {

    Optional<DirectoryUser> findUser(String username);

    /**
     * Returns {@code true} when the user belongs to at least one group exposed by
     * the GroupProvider currently configured in Openfire.
     */
    boolean isMemberOfConfiguredGroupSet(String username);
}
