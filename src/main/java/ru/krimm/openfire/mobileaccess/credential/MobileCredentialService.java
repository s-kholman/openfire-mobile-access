package ru.krimm.openfire.mobileaccess.credential;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import ru.krimm.openfire.mobileaccess.directory.DirectoryEligibilityService;
import ru.krimm.openfire.mobileaccess.directory.EligibilityResult;

public final class MobileCredentialService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MobileCredentialService.class);

    private final DirectoryEligibilityService eligibilityService;
    private final MobileCredentialRepository repository;
    private final Pbkdf2PasswordHasher passwordHasher;
    private final Clock clock;

    public MobileCredentialService(
        final DirectoryEligibilityService eligibilityService,
        final MobileCredentialRepository repository,
        final Pbkdf2PasswordHasher passwordHasher,
        final Clock clock
    ) {
        this.eligibilityService = Objects.requireNonNull(eligibilityService);
        this.repository = Objects.requireNonNull(repository);
        this.passwordHasher = Objects.requireNonNull(passwordHasher);
        this.clock = Objects.requireNonNull(clock);
    }

    public void setPassword(final String username, final char[] password) {
        final String normalizedUsername = normalize(username);
        LOGGER.info("[RID:{}] Credential service setPassword started: username={}, passwordLength={}", rid(), normalizedUsername, password == null ? -1 : password.length);
        requireEligible(normalizedUsername);
        LOGGER.info("[RID:{}] Directory eligibility passed: username={}", rid(), normalizedUsername);
        validatePassword(password);
        LOGGER.info("[RID:{}] Password validation passed: username={}", rid(), normalizedUsername);
        repository.save(normalizedUsername, passwordHasher.hash(password), Instant.now(clock));
        LOGGER.info("[RID:{}] Credential repository save completed: username={}", rid(), normalizedUsername);
    }

    public void revoke(final String username) {
        repository.revoke(requireExisting(username), Instant.now(clock));
    }

    public void enable(final String username) {
        final String normalizedUsername = requireExisting(username);
        requireEligible(normalizedUsername);
        repository.enable(normalizedUsername, Instant.now(clock));
    }

    public void delete(final String username) {
        repository.delete(requireExisting(username));
    }

    public Optional<MobileCredentialRecord> find(final String username) {
        return repository.find(normalize(username));
    }

    public List<MobileCredentialRecord> findAll() {
        return repository.findAll();
    }

    private void requireEligible(final String username) {
        LOGGER.info("[RID:{}] Evaluating directory eligibility: username={}", rid(), username);
        final EligibilityResult eligibility = eligibilityService.evaluate(username);
        LOGGER.info("[RID:{}] Directory eligibility result: username={}, status={}, eligible={}", rid(), username, eligibility.status(), eligibility.isEligible());
        if (!eligibility.isEligible()) {
            throw new IllegalArgumentException("User is not eligible for mobile access: " + eligibility.status());
        }
    }

    private String requireExisting(final String username) {
        final String normalizedUsername = normalize(username);
        if (repository.find(normalizedUsername).isEmpty()) {
            throw new IllegalArgumentException("Mobile credential does not exist for user: " + normalizedUsername);
        }
        return normalizedUsername;
    }

    private static String normalize(final String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username must not be blank");
        }
        String value = username.trim();
        final int slash = value.lastIndexOf('\\');
        if (slash >= 0) {
            value = value.substring(slash + 1);
        }
        final int at = value.indexOf('@');
        if (at >= 0) {
            value = value.substring(0, at);
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException("Username must contain an account name");
        }
        return value.toLowerCase(Locale.ROOT);
    }

    private static void validatePassword(final char[] password) {
        if (password == null || password.length < 12) {
            throw new IllegalArgumentException("Mobile password must contain at least 12 characters");
        }
        if (password.length > 256) {
            throw new IllegalArgumentException("Mobile password is too long");
        }
    }

    private static String rid() {
        final String value = MDC.get("mobileAccessRequestId");
        return value == null ? "none" : value;
    }
}
