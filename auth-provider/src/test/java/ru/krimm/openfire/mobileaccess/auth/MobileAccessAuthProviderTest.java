package ru.krimm.openfire.mobileaccess.auth;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.junit.jupiter.api.Test;

class MobileAccessAuthProviderTest {

    @Test
    void acceptsExistingDirectoryUserWithoutCheckingGroups() {
        final MobileAccessAuthProvider provider = new MobileAccessAuthProvider(username -> {
            assertEquals("a_bertram", username);
        });

        assertDoesNotThrow(() -> provider.requireDirectoryEligibility("a_bertram"));
    }

    @Test
    void rejectsUserMissingFromConfiguredUserProvider() {
        final MobileAccessAuthProvider provider = new MobileAccessAuthProvider(username -> {
            throw new UserNotFoundException();
        });

        assertThrows(
            UnauthorizedException.class,
            () -> provider.requireDirectoryEligibility("missing")
        );
    }

    @Test
    void normalizesPlainUsername() {
        assertEquals("a_bertram", MobileAccessAuthProvider.normalizeUsername(" A_BERTRAM "));
    }

    @Test
    void normalizesDomainQualifiedUsername() {
        assertEquals("a_bertram", MobileAccessAuthProvider.normalizeUsername("KRIMM\\A_BERTRAM"));
    }

    @Test
    void normalizesJidStyleUsername() {
        assertEquals("a_bertram", MobileAccessAuthProvider.normalizeUsername("A_BERTRAM@krimm.local"));
    }

    @Test
    void rejectsUsernameWithoutAccountName() {
        assertThrows(
            IllegalArgumentException.class,
            () -> MobileAccessAuthProvider.normalizeUsername("@krimm.local")
        );
    }
}
