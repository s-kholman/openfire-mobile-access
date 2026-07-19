package ru.krimm.openfire.mobileaccess.credential;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MobileCredentialRepository {

    void save(String username, Pbkdf2PasswordHasher.HashedPassword password, Instant changedAt);

    void revoke(String username, Instant revokedAt);

    void enable(String username, Instant changedAt);

    void delete(String username);

    Optional<MobileCredentialRecord> find(String username);

    List<MobileCredentialRecord> findAll();
}