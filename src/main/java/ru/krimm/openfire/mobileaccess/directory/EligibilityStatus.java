package ru.krimm.openfire.mobileaccess.directory;

/** Result of validating whether a directory user may receive a mobile credential. */
public enum EligibilityStatus {
    ELIGIBLE,
    USER_NOT_FOUND,
    GROUP_NOT_FOUND,
    USER_NOT_IN_ALLOWED_GROUP
}
