package ru.krimm.openfire.mobileaccess.auth;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

final class Pbkdf2CredentialVerifier {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int KEY_BITS = 256;
    private static final int MIN_ITERATIONS = 100_000;
    private static final int MAX_ITERATIONS = 2_000_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BYTES = KEY_BITS / 8;

    boolean verify(final char[] password, final StoredCredential credential) {
        if (password == null || credential == null || !credential.enabled()
                || !ALGORITHM.equals(credential.algorithm())
                || credential.iterations() < MIN_ITERATIONS
                || credential.iterations() > MAX_ITERATIONS) {
            return false;
        }
        try {
            final byte[] salt = Base64.getDecoder().decode(credential.salt());
            final byte[] expected = Base64.getDecoder().decode(credential.hash());
            if (salt.length != SALT_BYTES || expected.length != HASH_BYTES) {
                return false;
            }
            final PBEKeySpec specification = new PBEKeySpec(password, salt, credential.iterations(), KEY_BITS);
            try {
                final byte[] actual = SecretKeyFactory.getInstance(ALGORITHM).generateSecret(specification).getEncoded();
                return MessageDigest.isEqual(expected, actual);
            } finally {
                specification.clearPassword();
            }
        } catch (final IllegalArgumentException | GeneralSecurityException e) {
            return false;
        }
    }
}
