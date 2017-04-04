/*
 * Copyright (C) 2011 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.opendatakit.common.security.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.opendatakit.common.persistence.client.exception.DatastoreFailureException;
import org.opendatakit.common.persistence.exception.ODKDatastoreException;
import org.opendatakit.common.security.SecurityBeanDefs;
import org.opendatakit.common.security.client.UserSecurityInfo;
import org.opendatakit.common.security.client.exception.AccessDeniedException;
import org.opendatakit.common.security.common.GrantedAuthorityName;
import org.opendatakit.common.security.spring.ActiveDirectoryLdapAuthenticationProvider;
import org.opendatakit.common.security.spring.AnonymousAuthenticationFilter;
import org.opendatakit.common.web.CallingContext;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Common utility methods extracted from the AccessConfigurationServlet so they
 * can be shared between the servlet and GWT server classes.
 *
 * @author mitchellsundt@gmail.com
 *
 */
public class SecurityServiceUtil {

  private static final Set<String> specialNames = new HashSet<String>();

  public static final GrantedAuthority anonAuth = new SimpleGrantedAuthority(
      GrantedAuthorityName.USER_IS_ANONYMOUS.name());

  public static void addGroupOrRoleAuthorities(Set<GrantedAuthority> mappedSet, String name) {
		if ( name.compareToIgnoreCase(GrantedAuthorityName.GROUP_DATA_COLLECTORS.name()) == 0 ) {
			mappedSet.add(new SimpleGrantedAuthority(GrantedAuthorityName.ROLE_USER.name()));
			mappedSet.add(new SimpleGrantedAuthority(GrantedAuthorityName.ROLE_DATA_COLLECTOR.name()));
		} else if ( name.compareToIgnoreCase(GrantedAuthorityName.GROUP_DATA_VIEWERS.name()) == 0 ) {
			mappedSet.add(new SimpleGrantedAuthority(GrantedAuthorityName.ROLE_USER.name()));
			mappedSet.add(new SimpleGrantedAuthority(GrantedAuthorityName.ROLE_DATA_VIEWER.name()));
		} else if ( name.compareToIgnoreCase(GrantedAuthorityName.GROUP_FORM_MANAGERS.name()) == 0 ) {
			mappedSet.add(new SimpleGrantedAuthority(GrantedAuthorityName.ROLE_USER.name()));
			mappedSet.add(new SimpleGrantedAuthority(GrantedAuthorityName.ROLE_DATA_VIEWER.name()));
			mappedSet.add(new SimpleGrantedAuthority(GrantedAuthorityName.ROLE_DATA_OWNER.name()));
		} else if ( name.compareToIgnoreCase(GrantedAuthorityName.GROUP_SYNCHRONIZE_TABLES.name()) == 0 ) {
			mappedSet.add(new SimpleGrantedAuthority(GrantedAuthorityName.ROLE_USER.name()));
			mappedSet.add(new SimpleGrantedAuthority(GrantedAuthorityName.ROLE_SYNCHRONIZE_TABLES.name()));
		} else if ( name.compareToIgnoreCase(GrantedAuthorityName.GROUP_SUPER_USER_TABLES.name()) == 0 ) {
			mappedSet.add(new SimpleGrantedAuthority(GrantedAuthorityName.ROLE_USER.name()));
			mappedSet.add(new SimpleGrantedAuthority(GrantedAuthorityName.ROLE_SYNCHRONIZE_TABLES.name()));
			mappedSet.add(new SimpleGrantedAuthority(GrantedAuthorityName.ROLE_SUPER_USER_TABLES.name()));
		} else if ( name.compareToIgnoreCase(GrantedAuthorityName.GROUP_ADMINISTER_TABLES.name()) == 0 ) {
			mappedSet.add(new SimpleGrantedAuthority(GrantedAuthorityName.ROLE_USER.name()));
			mappedSet.add(new SimpleGrantedAuthority(GrantedAuthorityName.ROLE_SYNCHRONIZE_TABLES.name()));
			mappedSet.add(new SimpleGrantedAuthority(GrantedAuthorityName.ROLE_SUPER_USER_TABLES.name()));
			mappedSet.add(new SimpleGrantedAuthority(GrantedAuthorityName.ROLE_ADMINISTER_TABLES.name()));
		} else if ( name.compareToIgnoreCase(GrantedAuthorityName.GROUP_SITE_ADMINS.name()) == 0 ) {
			mappedSet.add(new SimpleGrantedAuthority(GrantedAuthorityName.ROLE_USER.name()));
			mappedSet.add(new SimpleGrantedAuthority(GrantedAuthorityName.ROLE_SYNCHRONIZE_TABLES.name()));
			mappedSet.add(new SimpleGrantedAuthority(GrantedAuthorityName.ROLE_SUPER_USER_TABLES.name()));
			mappedSet.add(new SimpleGrantedAuthority(GrantedAuthorityName.ROLE_ADMINISTER_TABLES.name()));
			mappedSet.add(new SimpleGrantedAuthority(GrantedAuthorityName.ROLE_DATA_COLLECTOR.name()));
			mappedSet.add(new SimpleGrantedAuthority(GrantedAuthorityName.ROLE_DATA_VIEWER.name()));
			mappedSet.add(new SimpleGrantedAuthority(GrantedAuthorityName.ROLE_DATA_OWNER.name()));
			mappedSet.add(new SimpleGrantedAuthority(GrantedAuthorityName.ROLE_SITE_ACCESS_ADMIN.name()));
		} else {
		  mappedSet.add(new SimpleGrantedAuthority(name));
		}
  }
  
  /**
   * Return all registered users and the Anonymous user.
   *
   * @param withAuthorities
   * @param cc
   * @return
   * @throws AccessDeniedException
   * @throws DatastoreFailureException
   */
  public static ArrayList<UserSecurityInfo> getAllUsers(boolean withAuthorities, CallingContext cc)
      throws AccessDeniedException, DatastoreFailureException {

	ActiveDirectoryLdapAuthenticationProvider provider = 
			  (ActiveDirectoryLdapAuthenticationProvider) 
			  cc.getBean(SecurityBeanDefs.ACTIVE_DIRECTORY_LDAP_AUTHENTICATION_PROVIDER);
	
	return provider.getAllUsers(withAuthorities, cc);
  }

  static GrantedAuthorityName mapName(GrantedAuthority auth, Set<GrantedAuthority> badGrants) {
    GrantedAuthorityName name = null;
    try {
      name = GrantedAuthorityName.valueOf(auth.getAuthority());
    } catch (Exception e) {
      badGrants.add(auth);
    }
    return name;
  }

  public static void setUserAuthenticationLists(UserSecurityInfo userInfo, 
        Collection<? extends GrantedAuthority> allGrants) {
    TreeSet<String> authorities = new TreeSet<String>();
    for (GrantedAuthority auth : allGrants) {
    	String name = auth.getAuthority();
    	if (name != null) {
    		authorities.add(name);
    	}
    }
    userInfo.setGrantedAuthorities(authorities);
  }

  public static void setAuthenticationListsForAnonymousUser(UserSecurityInfo userInfo,
      CallingContext cc) {

    // The assigned groups are the specialGroup that this user defines
    // (i.e., anonymous or daemon) plus all directly-assigned assignable
    // permissions.
    AnonymousAuthenticationFilter filter = 
            (AnonymousAuthenticationFilter) 
            cc.getBean(SecurityBeanDefs.ANONYMOUS_AUTHENTICATION_FILTER);
    

    Set<GrantedAuthority> auths = filter.getAuthorities(cc);

    SecurityServiceUtil.setUserAuthenticationLists(userInfo, auths);
  }

  /**
   * Get the complete set of granted authorities (ROLE and RUN_AS grants) this user possesses.
   * 
   * @param cc
   * @return
   * @throws ODKDatastoreException
   */
  public static TreeSet<String> getCurrentUserSecurityInfo(CallingContext cc)
      throws ODKDatastoreException {
    TreeSet<String> authorities = new TreeSet<String>();
    Set<GrantedAuthority> grants = new HashSet<GrantedAuthority>();
    grants.addAll(cc.getCurrentUser().getAuthorities());
    for ( GrantedAuthority a : grants ) {
      authorities.add(a.getAuthority());
    }
    return authorities;
  }

  public static final synchronized boolean isSpecialName(String authority) {
    if (SecurityServiceUtil.specialNames.isEmpty()) {
      for (GrantedAuthorityName n : GrantedAuthorityName.values()) {
        SecurityServiceUtil.specialNames.add(n.name());
      }
    }

    return SecurityServiceUtil.specialNames.contains(authority)
        || authority.startsWith(GrantedAuthorityName.RUN_AS_PREFIX)
        || authority.startsWith(GrantedAuthorityName.ROLE_PREFIX);
  }
}
