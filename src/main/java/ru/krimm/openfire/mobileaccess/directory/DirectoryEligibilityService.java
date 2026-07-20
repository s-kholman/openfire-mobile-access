package ru.krimm.openfire.mobileaccess.directory;

import java.util.Objects;
import java.util.Optional;

/**
 * Applies the directory-side policy for issuing a local mobile credential.
 *
 * <p>The configured Openfire UserProvider is authoritative for eligibility. In the LDAP
 * deployment, {@code ldap.searchFilter} already restricts visible users to enabled members
 * of the allowed AD group and supports nested group membership through the AD matching rule.
 * Re-checking membership through GroupProvider would incorrectly reject nested members.</p>
 */
public final class DirectoryEligibilityService {

    private final DirectoryGateway directoryGateway;

    public DirectoryEligibilityService(final DirectoryGateway directoryGateway) {
        this.directoryGateway = Objects.requireNonNull(directoryGateway, "directoryGateway must not be null");
    }

    public EligibilityResult evaluate(final String username) {
        final Optional<DirectoryUser> user = directoryGateway.findUser(username);
        if (user.isEmpty()) {
            return EligibilityResult.rejected(EligibilityStatus.USER_NOT_FOUND);
        }
        return EligibilityResult.eligible(user.get());
    }
}
