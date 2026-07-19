package ru.krimm.openfire.mobileaccess;

import java.io.File;
import java.time.Clock;
import java.util.Objects;

import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.krimm.openfire.mobileaccess.admin.MobileAccessAdministrationService;
import ru.krimm.openfire.mobileaccess.audit.JdbcAuditRepository;
import ru.krimm.openfire.mobileaccess.credential.JdbcMobileCredentialRepository;
import ru.krimm.openfire.mobileaccess.credential.MobileCredentialService;
import ru.krimm.openfire.mobileaccess.credential.Pbkdf2PasswordHasher;
import ru.krimm.openfire.mobileaccess.directory.DirectoryEligibilityService;
import ru.krimm.openfire.mobileaccess.directory.OpenfireDirectoryGateway;

/** Entry point for the Mobile Access Openfire plugin. */
public final class MobileAccessPlugin implements Plugin {

    public static final String VERSION = "0.2.6-SNAPSHOT";

    static final String ALLOWED_GROUP_PROPERTY = "plugin.mobileaccess.allowedGroup";
    static final String DEFAULT_ALLOWED_GROUP = "Openfire-Users";

    private static final Logger LOGGER = LoggerFactory.getLogger(MobileAccessPlugin.class);
    private static volatile MobileAccessPlugin instance;

    private File pluginDirectory;
    private DirectoryEligibilityService directoryEligibilityService;
    private MobileCredentialService mobileCredentialService;
    private MobileAccessAdministrationService administrationService;

    @Override
    public void initializePlugin(final PluginManager pluginManager, final File pluginDirectory) {
        Objects.requireNonNull(pluginManager, "pluginManager must not be null");
        this.pluginDirectory = Objects.requireNonNull(pluginDirectory, "pluginDirectory must not be null");

        final Clock clock = Clock.systemUTC();
        final String allowedGroup = JiveGlobals.getProperty(ALLOWED_GROUP_PROPERTY, DEFAULT_ALLOWED_GROUP);
        directoryEligibilityService = new DirectoryEligibilityService(new OpenfireDirectoryGateway(), allowedGroup);
        mobileCredentialService = new MobileCredentialService(
            directoryEligibilityService,
            new JdbcMobileCredentialRepository(),
            new Pbkdf2PasswordHasher(),
            clock
        );
        administrationService = new MobileAccessAdministrationService(
            mobileCredentialService,
            new JdbcAuditRepository(),
            clock
        );
        instance = this;

        LOGGER.info(
            "Mobile Access plugin {} initialized from {} with allowed directory group '{}'",
            VERSION,
            pluginDirectory.getAbsolutePath(),
            allowedGroup
        );
    }

    @Override
    public void destroyPlugin() {
        LOGGER.info("Mobile Access plugin {} destroyed", VERSION);
        instance = null;
        administrationService = null;
        mobileCredentialService = null;
        directoryEligibilityService = null;
        pluginDirectory = null;
    }

    public static MobileAccessAdministrationService administrationService() {
        final MobileAccessPlugin current = instance;
        if (current == null || current.administrationService == null) {
            throw new IllegalStateException("Mobile Access plugin is not initialized");
        }
        return current.administrationService;
    }

    File getPluginDirectory() {
        return pluginDirectory;
    }

    DirectoryEligibilityService getDirectoryEligibilityService() {
        return directoryEligibilityService;
    }

    MobileCredentialService getMobileCredentialService() {
        return mobileCredentialService;
    }
}
