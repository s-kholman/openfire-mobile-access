package ru.krimm.openfire.mobileaccess.admin;

import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import ru.krimm.openfire.mobileaccess.audit.AuditAction;
import ru.krimm.openfire.mobileaccess.audit.AuditOutcome;
import ru.krimm.openfire.mobileaccess.audit.JdbcAuditRepository;
import ru.krimm.openfire.mobileaccess.credential.MobileCredentialService;

/** Coordinates credential changes with mandatory audit recording. */
public final class MobileAccessAdministrationService {

    private final MobileCredentialService credentialService;
    private final JdbcAuditRepository auditRepository;
    private final Clock clock;

    public MobileAccessAdministrationService(
        final MobileCredentialService credentialService,
        final JdbcAuditRepository auditRepository,
        final Clock clock
    ) {
        this.credentialService = Objects.requireNonNull(credentialService);
        this.auditRepository = Objects.requireNonNull(auditRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    public void setPassword(final String actor, final String username, final char[] password) {
        try {
            credentialService.setPassword(username, password);
            auditRepository.record(Instant.now(clock), actor, AuditAction.SET_PASSWORD, username, AuditOutcome.SUCCESS, null);
        } catch (final RuntimeException e) {
            recordFailure(actor, AuditAction.SET_PASSWORD, username, e);
            throw e;
        } finally {
            if (password != null) {
                Arrays.fill(password, '\0');
            }
        }
    }

    public void revoke(final String actor, final String username) {
        try {
            credentialService.revoke(username);
            auditRepository.record(Instant.now(clock), actor, AuditAction.REVOKE_PASSWORD, username, AuditOutcome.SUCCESS, null);
        } catch (final RuntimeException e) {
            recordFailure(actor, AuditAction.REVOKE_PASSWORD, username, e);
            throw e;
        }
    }

    private void recordFailure(
        final String actor,
        final AuditAction action,
        final String username,
        final RuntimeException failure
    ) {
        try {
            auditRepository.record(
                Instant.now(clock), actor, action, username, AuditOutcome.FAILURE,
                failure.getClass().getSimpleName() + ": " + failure.getMessage()
            );
        } catch (final RuntimeException auditFailure) {
            failure.addSuppressed(auditFailure);
        }
    }
}
