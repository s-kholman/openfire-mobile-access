package ru.krimm.openfire.mobileaccess.directory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.junit.Test;

public class DirectoryEligibilityServiceTest {

    @Test
    public void returnsEligibleForMemberOfConfiguredGroupSet() {
        final FakeDirectoryGateway gateway = new FakeDirectoryGateway();
        gateway.user = Optional.of(new DirectoryUser("ivanov", "Ivan Ivanov", "ivanov@example.test"));
        gateway.member = true;

        final EligibilityResult result = new DirectoryEligibilityService(gateway)
            .evaluate(" IVANOV ");

        assertTrue(result.isEligible());
        assertEquals("ivanov", result.optionalUser().orElseThrow().username());
    }

    @Test
    public void rejectsUnknownUserBeforeCheckingGroups() {
        final FakeDirectoryGateway gateway = new FakeDirectoryGateway();

        final EligibilityResult result = new DirectoryEligibilityService(gateway)
            .evaluate("missing");

        assertEquals(EligibilityStatus.USER_NOT_FOUND, result.status());
        assertFalse(gateway.groupsChecked);
    }

    @Test
    public void rejectsUserOutsideConfiguredGroupSet() {
        final FakeDirectoryGateway gateway = new FakeDirectoryGateway();
        gateway.user = Optional.of(new DirectoryUser("ivanov", null, null));

        final EligibilityResult result = new DirectoryEligibilityService(gateway)
            .evaluate("ivanov");

        assertEquals(EligibilityStatus.USER_NOT_IN_ALLOWED_GROUP, result.status());
        assertTrue(gateway.groupsChecked);
    }

    private static final class FakeDirectoryGateway implements DirectoryGateway {
        private Optional<DirectoryUser> user = Optional.empty();
        private boolean member;
        private boolean groupsChecked;

        @Override
        public Optional<DirectoryUser> findUser(final String username) {
            return user;
        }

        @Override
        public boolean isMemberOfConfiguredGroupSet(final String username) {
            groupsChecked = true;
            return member;
        }
    }
}
