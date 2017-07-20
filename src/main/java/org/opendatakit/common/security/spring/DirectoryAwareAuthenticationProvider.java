package org.opendatakit.common.security.spring;

import org.opendatakit.common.persistence.client.exception.DatastoreFailureException;
import org.opendatakit.common.security.client.UserSecurityInfo;
import org.opendatakit.common.security.client.exception.AccessDeniedException;
import org.opendatakit.common.web.CallingContext;

import java.util.List;

/**
 * Authentication Provider that is capable of reading the underlying user directory.
 */
public interface DirectoryAwareAuthenticationProvider {
    /**
     *
     * @param cc
     * @return the defaultGroup of the current user or null.
     */
    String getDefaultGroup(CallingContext cc);

    /**
     * Return all registered users and the Anonymous user.
     * Invoked from SecurityServiceUtils
     *
     * @param withAuthorities
     * @param cc
     * @return
     */
    List<UserSecurityInfo> getAllUsers(boolean withAuthorities, CallingContext cc) throws AccessDeniedException, DatastoreFailureException;
}
