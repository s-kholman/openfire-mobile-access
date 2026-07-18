package ru.krimm.openfire.mobileaccess.directory;

import java.util.Objects;
import java.util.Optional;

/**
 * Applies the directory-side policy for issuing a local mobile credential.
 */
public final class DirectoryEligibilityService {

    private final DirectoryGateway directoryGateway;
    private final String allowedGroupName;

    public DirectoryEligibilityService(
        final DirectoryGateway directoryGateway,
        final String allowedGroupName
    ) {
        this.directoryGateway = Objects.requireNonNull(directoryGateway, "directoryGateway must not be null");
        if (allowedGroupName == null || allowedGroupName.isBlank()) {
            throw new IllegalArgumentException("allowedGroupName must not be blank");
        }
        this.allowedGroupName = allowedGroupName.trim();
    }

    public EligibilityResult evaluate(final String username) {
        final Optional<DirectoryUser> user = directoryGateway.findUser(username);
        if (user.isEmpty()) {
            return EligibilityResult.rejected(EligibilityStatus.USER_NOT_FOUND);
        }
        if (!directoryGateway.groupExists(allowedGroupName)) {
            return EligibilityResult.rejected(EligibilityStatus.GROUP_NOT_FOUND);
        }
        if (!directoryGateway.isMemberOf(user.get().username(), allowedGroupName)) {
            return EligibilityResult.rejected(EligibilityStatus.USER_NOT_IN_ALLOWED_GROUP);
        }
        return EligibilityResult.eligible(user.get());
    }

    public String getAllowedGroupName() {
        return allowedGroupName;
    }
}
