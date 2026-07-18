package ru.krimm.openfire.mobileaccess.auth;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

final class Pbkdf2CredentialVerifier {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int KEY_BITS = 256;

    boolean verify(final char[] password, final StoredCredential credential) {
        if (!credential.enabled() || !ALGORITHM.equals(credential.algorithm())) {
            return false;
        }
        try {
            final byte[] salt = Base64.getDecoder().decode(credential.salt());
            final byte[] expected = Base64.getDecoder().decode(credential.hash());
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
