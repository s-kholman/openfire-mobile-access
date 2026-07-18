package ru.krimm.openfire.mobileaccess.credential;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class Pbkdf2PasswordHasherTest {

    @Test
    public void createsUniqueSaltedHashes() {
        final Pbkdf2PasswordHasher hasher = new Pbkdf2PasswordHasher();
        final var first = hasher.hash("correct horse battery staple".toCharArray());
        final var second = hasher.hash("correct horse battery staple".toCharArray());

        assertEquals(Pbkdf2PasswordHasher.ALGORITHM, first.algorithm());
        assertNotEquals(first.salt(), second.salt());
        assertNotEquals(first.hash(), second.hash());
    }

    @Test
    public void rejectsEmptyPassword() {
        final Pbkdf2PasswordHasher hasher = new Pbkdf2PasswordHasher();
        assertThrows(IllegalArgumentException.class, () -> hasher.hash(new char[0]));
    }
}
