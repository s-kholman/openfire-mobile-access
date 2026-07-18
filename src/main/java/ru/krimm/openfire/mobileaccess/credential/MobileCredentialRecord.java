package ru.krimm.openfire.mobileaccess.credential;

import java.time.Instant;

public record MobileCredentialRecord(
    String username,
    String algorithm,
    int iterations,
    String salt,
    String hash,
    boolean enabled,
    Instant updatedAt,
    Instant revokedAt
) {
}
