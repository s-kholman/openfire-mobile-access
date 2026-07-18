package ru.krimm.openfire.mobileaccess.credential;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
        LOGGER.info("MobileCredentialService.setPassword entered for target={}", username);
        final String normalizedUsername = normalize(username);
        LOGGER.info("Evaluating directory eligibility for target={}", normalizedUsername);
        final EligibilityResult eligibility = eligibilityService.evaluate(normalizedUsername);
        LOGGER.info(
            "Directory eligibility evaluated for target={}: status={}, eligible={}",
            normalizedUsername,
            eligibility.status(),
            eligibility.isEligible()
        );
        if (!eligibility.isEligible()) {
            throw new IllegalArgumentException("User is not eligible for mobile access: " + eligibility.status());
        }

        validatePassword(password);
        LOGGER.info("Mobile password validation completed for target={}", normalizedUsername);

        final Pbkdf2PasswordHasher.HashedPassword hashedPassword = passwordHasher.hash(password);
        LOGGER.info(
            "Mobile password hashing completed for target={}: algorithm={}, iterations={}",
            normalizedUsername,
            hashedPassword.algorithm(),
            hashedPassword.iterations()
        );

        LOGGER.info("Calling MobileCredentialRepository.save for target={}", normalizedUsername);
        repository.save(normalizedUsername, hashedPassword, Instant.now(clock));
        LOGGER.info("MobileCredentialRepository.save completed for target={}", normalizedUsername);
    }

    public void revoke(final String username) {
        final String normalizedUsername = normalize(username);
        LOGGER.info("Calling MobileCredentialRepository.revoke for target={}", normalizedUsername);
        repository.revoke(normalizedUsername, Instant.now(clock));
        LOGGER.info("MobileCredentialRepository.revoke completed for target={}", normalizedUsername);
    }

    private static String normalize(final String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username must not be blank");
        }
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private static void validatePassword(final char[] password) {
        if (password == null || password.length < 12) {
            throw new IllegalArgumentException("Mobile password must contain at least 12 characters");
        }
        if (password.length > 256) {
            throw new IllegalArgumentException("Mobile password is too long");
        }
    }
}
