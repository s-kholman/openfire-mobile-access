package ru.krimm.openfire.mobileaccess.directory;

import java.util.Optional;

/**
 * Validation result with the resolved directory user when validation succeeds.
 */
public record EligibilityResult(EligibilityStatus status, DirectoryUser user) {

    public static EligibilityResult eligible(final DirectoryUser user) {
        return new EligibilityResult(EligibilityStatus.ELIGIBLE, user);
    }

    public static EligibilityResult rejected(final EligibilityStatus status) {
        if (status == EligibilityStatus.ELIGIBLE) {
            throw new IllegalArgumentException("Use eligible(user) for an eligible result");
        }
        return new EligibilityResult(status, null);
    }

    public boolean isEligible() {
        return status == EligibilityStatus.ELIGIBLE;
    }

    public Optional<DirectoryUser> optionalUser() {
        return Optional.ofNullable(user);
    }
}
