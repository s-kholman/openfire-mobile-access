package ru.krimm.openfire.mobileaccess.directory;

import java.util.Optional;

/** Boundary between plugin business logic and the configured Openfire UserProvider. */
public interface DirectoryGateway {

    Optional<DirectoryUser> findUser(String username);
}
