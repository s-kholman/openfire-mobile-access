package ru.krimm.openfire.mobileaccess.directory;

import java.util.Objects;

/**
 * Immutable projection of an Openfire user supplied by the configured user provider.
 */
public record DirectoryUser(String username, String name, String email) {

    public DirectoryUser {
        username = Objects.requireNonNull(username, "username must not be null");
    }
}
