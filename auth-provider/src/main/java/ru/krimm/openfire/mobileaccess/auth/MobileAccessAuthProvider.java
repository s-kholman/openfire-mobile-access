package ru.krimm.openfire.mobileaccess.auth;

import org.jivesoftware.openfire.auth.AuthProvider;
import org.jivesoftware.openfire.auth.ConnectionException;
import org.jivesoftware.openfire.auth.InternalUnauthenticatedException;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.user.UserNotFoundException;

/**
 * Authentication provider that will validate local mobile credentials.
 *
 * <p>This class must be installed on the Openfire core classpath, not only inside
 * the plugin classloader. Credential persistence and verification are introduced
 * in a separate change.</p>
 */
public final class MobileAccessAuthProvider implements AuthProvider {

    @Override
    public void authenticate(final String username, final String password)
            throws UnauthorizedException, ConnectionException, InternalUnauthenticatedException {
        throw new UnauthorizedException("Mobile Access credential verification is not configured yet");
    }

    @Override
    public String getPassword(final String username) throws UserNotFoundException {
        throw new UnsupportedOperationException("Mobile passwords are never retrievable");
    }

    @Override
    public void setPassword(final String username, final String password) throws UserNotFoundException {
        throw new UnsupportedOperationException("Passwords are managed by the Mobile Access plugin");
    }

    @Override
    public boolean supportsPasswordRetrieval() {
        return false;
    }

    @Override
    public boolean isScramSupported() {
        return false;
    }

    @Override
    public String getSalt(final String username) throws UserNotFoundException {
        throw new UnsupportedOperationException("SCRAM is not supported");
    }

    @Override
    public int getIterations(final String username) throws UserNotFoundException {
        throw new UnsupportedOperationException("SCRAM is not supported");
    }

    @Override
    public String getServerKey(final String username) throws UserNotFoundException {
        throw new UnsupportedOperationException("SCRAM is not supported");
    }

    @Override
    public String getStoredKey(final String username) throws UserNotFoundException {
        throw new UnsupportedOperationException("SCRAM is not supported");
    }
}
