package ru.krimm.openfire.mobileaccess.directory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.junit.Test;

public class DirectoryEligibilityServiceTest {

    @Test
    public void returnsEligibleForExistingAllowedGroupMember() {
        final FakeDirectoryGateway gateway = new FakeDirectoryGateway();
        gateway.user = Optional.of(new DirectoryUser("ivanov", "Ivan Ivanov", "ivanov@example.test"));
        gateway.groupExists = true;
        gateway.member = true;

        final EligibilityResult result = new DirectoryEligibilityService(gateway, "Openfire-Users")
            .evaluate(" IVANOV ");

        assertTrue(result.isEligible());
        assertEquals("ivanov", result.optionalUser().orElseThrow().username());
    }

    @Test
    public void rejectsUnknownUserBeforeCheckingGroup() {
        final FakeDirectoryGateway gateway = new FakeDirectoryGateway();

        final EligibilityResult result = new DirectoryEligibilityService(gateway, "Openfire-Users")
            .evaluate("missing");

        assertEquals(EligibilityStatus.USER_NOT_FOUND, result.status());
        assertFalse(gateway.groupChecked);
    }

    @Test
    public void rejectsMissingAllowedGroup() {
        final FakeDirectoryGateway gateway = new FakeDirectoryGateway();
        gateway.user = Optional.of(new DirectoryUser("ivanov", null, null));

        final EligibilityResult result = new DirectoryEligibilityService(gateway, "Openfire-Users")
            .evaluate("ivanov");

        assertEquals(EligibilityStatus.GROUP_NOT_FOUND, result.status());
    }

    @Test
    public void rejectsUserOutsideAllowedGroup() {
        final FakeDirectoryGateway gateway = new FakeDirectoryGateway();
        gateway.user = Optional.of(new DirectoryUser("ivanov", null, null));
        gateway.groupExists = true;

        final EligibilityResult result = new DirectoryEligibilityService(gateway, "Openfire-Users")
            .evaluate("ivanov");

        assertEquals(EligibilityStatus.USER_NOT_IN_ALLOWED_GROUP, result.status());
    }

    private static final class FakeDirectoryGateway implements DirectoryGateway {
        private Optional<DirectoryUser> user = Optional.empty();
        private boolean groupExists;
        private boolean member;
        private boolean groupChecked;

        @Override
        public Optional<DirectoryUser> findUser(final String username) {
            return user;
        }

        @Override
        public boolean groupExists(final String groupName) {
            groupChecked = true;
            return groupExists;
        }

        @Override
        public boolean isMemberOf(final String username, final String groupName) {
            return member;
        }
    }
}
