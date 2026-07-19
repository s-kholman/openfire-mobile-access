package ru.krimm.openfire.mobileaccess.directory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.junit.Test;

public class DirectoryEligibilityServiceTest {

    @Test
    public void returnsEligibleForUserVisibleThroughConfiguredProvider() {
        final FakeDirectoryGateway gateway = new FakeDirectoryGateway();
        gateway.user = Optional.of(new DirectoryUser("ivanov", "Ivan Ivanov", "ivanov@example.test"));

        final EligibilityResult result = new DirectoryEligibilityService(gateway)
            .evaluate(" IVANOV ");

        assertTrue(result.isEligible());
        assertEquals("ivanov", result.optionalUser().orElseThrow().username());
    }

    @Test
    public void rejectsUserHiddenByConfiguredProvider() {
        final FakeDirectoryGateway gateway = new FakeDirectoryGateway();

        final EligibilityResult result = new DirectoryEligibilityService(gateway)
            .evaluate("missing");

        assertEquals(EligibilityStatus.USER_NOT_FOUND, result.status());
    }

    private static final class FakeDirectoryGateway implements DirectoryGateway {
        private Optional<DirectoryUser> user = Optional.empty();

        @Override
        public Optional<DirectoryUser> findUser(final String username) {
            return user;
        }
    }
}
