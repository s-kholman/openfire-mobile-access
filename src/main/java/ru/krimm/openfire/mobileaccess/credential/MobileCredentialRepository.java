package ru.krimm.openfire.mobileaccess.credential;

import java.time.Instant;
import java.util.Optional;

public interface MobileCredentialRepository {

    void save(String username, Pbkdf2PasswordHasher.HashedPassword password, Instant changedAt);

    void revoke(String username, Instant revokedAt);

    Optional<MobileCredentialRecord> find(String username);
}
