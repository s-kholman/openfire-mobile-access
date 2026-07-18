package ru.krimm.openfire.mobileaccess.directory;

import java.util.Objects;
import java.util.Optional;

/** Applies the directory-side policy for issuing a local mobile credential. */
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
        if (!directoryGateway.isMemberOfConfiguredGroupSet(user.get().username())) {
            return EligibilityResult.rejected(EligibilityStatus.USER_NOT_IN_ALLOWED_GROUP);
        }
        return EligibilityResult.eligible(user.get());
    }
}
