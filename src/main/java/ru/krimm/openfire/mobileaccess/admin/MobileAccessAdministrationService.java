package ru.krimm.openfire.mobileaccess.admin;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.jivesoftware.util.JiveGlobals;
import ru.krimm.openfire.mobileaccess.audit.AuditAction;
import ru.krimm.openfire.mobileaccess.audit.AuditOutcome;
import ru.krimm.openfire.mobileaccess.audit.JdbcAuditRepository;
import ru.krimm.openfire.mobileaccess.credential.MobileCredentialRecord;
import ru.krimm.openfire.mobileaccess.credential.MobileCredentialService;

/** Coordinates credential and administrator changes with mandatory audit recording. */
public final class MobileAccessAdministrationService {

    private static final String AUTHORIZED_USERS_PROPERTY = "admin.authorizedUsernames";

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

    public List<ManagedMobileUser> listUsers() {
        final Set<String> administrators = readAdministrators();
        final List<ManagedMobileUser> users = new ArrayList<>();
        for (final MobileCredentialRecord credential : credentialService.findAll()) {
            users.add(new ManagedMobileUser(
                credential.username(), credential.enabled(), administrators.contains(credential.username()),
                credential.updatedAt(), credential.revokedAt()
            ));
        }
        return users;
    }

    public void setPassword(final String actor, final String username, final char[] password) {
        execute(actor, AuditAction.SET_PASSWORD, username, () -> credentialService.setPassword(username, password));
        if (password != null) {
            Arrays.fill(password, '\0');
        }
    }

    public void block(final String actor, final String username) {
        execute(actor, AuditAction.BLOCK_ACCESS, username, () -> credentialService.revoke(username));
    }

    public void enable(final String actor, final String username) {
        execute(actor, AuditAction.ENABLE_ACCESS, username, () -> credentialService.enable(username));
    }

    public void delete(final String actor, final String username) {
        final String normalized = normalize(username);
        execute(actor, AuditAction.DELETE_CREDENTIAL, normalized, () -> {
            credentialService.delete(normalized);
            final Set<String> administrators = readAdministrators();
            if (administrators.remove(normalized)) {
                writeAdministrators(administrators);
            }
        });
    }

    public void setAdministrator(final String actor, final String username, final boolean administrator) {
        final String normalized = normalize(username);
        final AuditAction action = administrator ? AuditAction.GRANT_ADMIN : AuditAction.REVOKE_ADMIN;
        execute(actor, action, normalized, () -> {
            final Set<String> administrators = readAdministrators();
            if (administrator) {
                administrators.add(normalized);
            } else {
                if (!administrators.contains(normalized)) {
                    return;
                }
                if (administrators.size() <= 1) {
                    throw new IllegalArgumentException("The last authorized administrator cannot be removed");
                }
                administrators.remove(normalized);
            }
            writeAdministrators(administrators);
        });
    }

    private void execute(final String actor, final AuditAction action, final String username, final Runnable operation) {
        try {
            operation.run();
            auditRepository.record(Instant.now(clock), actor, action, username, AuditOutcome.SUCCESS, null);
        } catch (final RuntimeException e) {
            recordFailure(actor, action, username, e);
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

    private static Set<String> readAdministrators() {
        final Set<String> result = new LinkedHashSet<>();
        final String value = JiveGlobals.getProperty(AUTHORIZED_USERS_PROPERTY, "");
        for (final String item : value.split(",")) {
            if (!item.isBlank()) {
                result.add(normalize(item));
            }
        }
        return result;
    }

    private static void writeAdministrators(final Set<String> administrators) {
        JiveGlobals.setProperty(AUTHORIZED_USERS_PROPERTY, String.join(",", administrators));
    }

    private static String normalize(final String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username must not be blank");
        }
        return username.trim().toLowerCase(Locale.ROOT);
    }

    public record ManagedMobileUser(
        String username,
        boolean enabled,
        boolean administrator,
        Instant updatedAt,
        Instant revokedAt
    ) {
    }
}