package ru.krimm.openfire.mobileaccess.admin;

import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.krimm.openfire.mobileaccess.audit.AuditAction;
import ru.krimm.openfire.mobileaccess.audit.AuditOutcome;
import ru.krimm.openfire.mobileaccess.audit.JdbcAuditRepository;
import ru.krimm.openfire.mobileaccess.credential.MobileCredentialService;

/** Coordinates credential changes with mandatory audit recording. */
public final class MobileAccessAdministrationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MobileAccessAdministrationService.class);

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
        LOGGER.info("Mobile password administration started: actor={}, target={}", actor, username);
        try {
            LOGGER.info("Calling MobileCredentialService.setPassword for target={}", username);
            credentialService.setPassword(username, password);
            LOGGER.info("MobileCredentialService.setPassword completed for target={}", username);

            LOGGER.info("Recording successful SET_PASSWORD audit event for target={}", username);
            auditRepository.record(Instant.now(clock), actor, AuditAction.SET_PASSWORD, username, AuditOutcome.SUCCESS, null);
            LOGGER.info("Successful SET_PASSWORD audit event recorded for target={}", username);
        } catch (final RuntimeException e) {
            LOGGER.error("Mobile password administration failed for target=" + username, e);
            recordFailure(actor, AuditAction.SET_PASSWORD, username, e);
            throw e;
        } finally {
            if (password != null) {
                Arrays.fill(password, '\0');
            }
        }
    }

    public void revoke(final String actor, final String username) {
        LOGGER.info("Mobile access revocation started: actor={}, target={}", actor, username);
        try {
            credentialService.revoke(username);
            LOGGER.info("MobileCredentialService.revoke completed for target={}", username);

            auditRepository.record(Instant.now(clock), actor, AuditAction.REVOKE_PASSWORD, username, AuditOutcome.SUCCESS, null);
            LOGGER.info("Successful REVOKE_PASSWORD audit event recorded for target={}", username);
        } catch (final RuntimeException e) {
            LOGGER.error("Mobile access revocation failed for target=" + username, e);
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
            LOGGER.info("Recording failed {} audit event for target={}", action, username);
            auditRepository.record(
                Instant.now(clock), actor, action, username, AuditOutcome.FAILURE,
                failure.getClass().getSimpleName() + ": " + failure.getMessage()
            );
            LOGGER.info("Failed {} audit event recorded for target={}", action, username);
        } catch (final RuntimeException auditFailure) {
            LOGGER.error("Unable to record failed " + action + " audit event for target=" + username, auditFailure);
            failure.addSuppressed(auditFailure);
        }
    }
}
