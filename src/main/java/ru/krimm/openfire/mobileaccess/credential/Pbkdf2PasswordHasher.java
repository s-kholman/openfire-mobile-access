package ru.krimm.openfire.mobileaccess.credential;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Creates salted PBKDF2-HMAC-SHA256 hashes for mobile passwords. */
public final class Pbkdf2PasswordHasher {

    public static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    public static final int DEFAULT_ITERATIONS = 310_000;
    public static final int SALT_BYTES = 16;
    public static final int KEY_BITS = 256;

    private final SecureRandom secureRandom;
    private final int iterations;

    public Pbkdf2PasswordHasher() {
        this(new SecureRandom(), DEFAULT_ITERATIONS);
    }

    Pbkdf2PasswordHasher(final SecureRandom secureRandom, final int iterations) {
        this.secureRandom = secureRandom;
        if (iterations < 100_000 || iterations > 2_000_000) {
            throw new IllegalArgumentException("PBKDF2 iteration count is outside the supported range");
        }
        this.iterations = iterations;
    }

    public HashedPassword hash(final char[] password) {
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("Password must not be empty");
        }

        final byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);
        final byte[] hash = derive(password, salt, iterations);
        return new HashedPassword(
            ALGORITHM,
            iterations,
            Base64.getEncoder().encodeToString(salt),
            Base64.getEncoder().encodeToString(hash)
        );
    }

    static byte[] derive(final char[] password, final byte[] salt, final int iterations) {
        final PBEKeySpec specification = new PBEKeySpec(password, salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(specification).getEncoded();
        } catch (final GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2-HMAC-SHA256 is unavailable", e);
        } finally {
            specification.clearPassword();
        }
    }

    public record HashedPassword(String algorithm, int iterations, String salt, String hash) {
    }
}
