package ru.krimm.openfire.mobileaccess.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.junit.jupiter.api.Test;

class Pbkdf2CredentialVerifierTest {

    @Test
    void acceptsCorrectPasswordAndRejectsIncorrectOrUnsafeCredentials() throws Exception {
        final String salt = Base64.getEncoder().encodeToString("0123456789abcdef".getBytes());
        final int iterations = 100_000;
        final PBEKeySpec spec = new PBEKeySpec(
            "a very strong password".toCharArray(),
            Base64.getDecoder().decode(salt),
            iterations,
            256
        );
        final String hash = Base64.getEncoder().encodeToString(
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded()
        );
        spec.clearPassword();

        final StoredCredential credential = new StoredCredential(
            "PBKDF2WithHmacSHA256", iterations, salt, hash, true
        );
        final Pbkdf2CredentialVerifier verifier = new Pbkdf2CredentialVerifier();

        assertTrue(verifier.verify("a very strong password".toCharArray(), credential));
        assertFalse(verifier.verify("wrong password".toCharArray(), credential));
        assertFalse(verifier.verify("a very strong password".toCharArray(),
            new StoredCredential(credential.algorithm(), credential.iterations(), credential.salt(), credential.hash(), false)));
        assertFalse(verifier.verify("a very strong password".toCharArray(),
            new StoredCredential(credential.algorithm(), 10, credential.salt(), credential.hash(), true)));
    }
}
