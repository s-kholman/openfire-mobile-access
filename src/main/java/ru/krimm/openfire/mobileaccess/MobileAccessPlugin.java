package ru.krimm.openfire.mobileaccess;

import java.io.File;
import java.time.Clock;
import java.util.Objects;

import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.krimm.openfire.mobileaccess.credential.JdbcMobileCredentialRepository;
import ru.krimm.openfire.mobileaccess.credential.MobileCredentialService;
import ru.krimm.openfire.mobileaccess.credential.Pbkdf2PasswordHasher;
import ru.krimm.openfire.mobileaccess.directory.DirectoryEligibilityService;
import ru.krimm.openfire.mobileaccess.directory.OpenfireDirectoryGateway;

/** Entry point for the Mobile Access Openfire plugin. */
public final class MobileAccessPlugin implements Plugin {

    static final String ALLOWED_GROUP_PROPERTY = "plugin.mobileaccess.allowedGroup";
    static final String DEFAULT_ALLOWED_GROUP = "Openfire-Users";

    private static final Logger LOGGER = LoggerFactory.getLogger(MobileAccessPlugin.class);

    private File pluginDirectory;
    private DirectoryEligibilityService directoryEligibilityService;
    private MobileCredentialService mobileCredentialService;

    @Override
    public void initializePlugin(final PluginManager pluginManager, final File pluginDirectory) {
        Objects.requireNonNull(pluginManager, "pluginManager must not be null");
        this.pluginDirectory = Objects.requireNonNull(pluginDirectory, "pluginDirectory must not be null");

        final String allowedGroup = JiveGlobals.getProperty(ALLOWED_GROUP_PROPERTY, DEFAULT_ALLOWED_GROUP);
        directoryEligibilityService = new DirectoryEligibilityService(new OpenfireDirectoryGateway(), allowedGroup);
        mobileCredentialService = new MobileCredentialService(
            directoryEligibilityService,
            new JdbcMobileCredentialRepository(),
            new Pbkdf2PasswordHasher(),
            Clock.systemUTC()
        );

        LOGGER.info(
            "Mobile Access plugin initialized from {} with allowed directory group '{}'",
            pluginDirectory.getAbsolutePath(),
            allowedGroup
        );
    }

    @Override
    public void destroyPlugin() {
        LOGGER.info("Mobile Access plugin destroyed");
        mobileCredentialService = null;
        directoryEligibilityService = null;
        pluginDirectory = null;
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
