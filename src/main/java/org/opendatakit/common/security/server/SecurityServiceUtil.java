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

import java.util.*;
import java.util.stream.Collectors;

import org.opendatakit.aggregate.odktables.rest.entity.PrivilegesInfo;
import org.opendatakit.aggregate.odktables.rest.entity.UserInfo;
import org.opendatakit.common.persistence.client.exception.DatastoreFailureException;
import org.opendatakit.common.persistence.exception.ODKDatastoreException;
import org.opendatakit.common.security.SecurityBeanDefs;
import org.opendatakit.common.security.User;
import org.opendatakit.common.security.client.UserSecurityInfo;
import org.opendatakit.common.security.client.UserSecurityInfo.UserType;
import org.opendatakit.common.security.client.exception.AccessDeniedException;
import org.opendatakit.common.security.common.EmailParser;
import org.opendatakit.common.security.common.GrantedAuthorityName;
import org.opendatakit.common.security.spring.AnonymousAuthenticationFilter;
import org.opendatakit.common.security.spring.DirectoryAwareAuthenticationProvider;
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

  private static final Map<String, String[]> impliedAuthorities;
  static {
    Map<String, String[]> authoritiesMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    authoritiesMap.put(GrantedAuthorityName.ROLE_DATA_OWNER.name(), new String[] {
            GrantedAuthorityName.ROLE_DATA_VIEWER.name()
    });
    authoritiesMap.put(GrantedAuthorityName.ROLE_SUPER_USER_TABLES.name(), new String[] {
            GrantedAuthorityName.ROLE_SYNCHRONIZE_TABLES.name()
    });
    authoritiesMap.put(GrantedAuthorityName.ROLE_ADMINISTER_TABLES.name(), new String[] {
            GrantedAuthorityName.ROLE_SYNCHRONIZE_TABLES.name(),
            GrantedAuthorityName.ROLE_SUPER_USER_TABLES.name()
    });
    authoritiesMap.put(GrantedAuthorityName.ROLE_SITE_ACCESS_ADMIN.name(), new String[] {
            GrantedAuthorityName.ROLE_SYNCHRONIZE_TABLES.name(),
            GrantedAuthorityName.ROLE_SUPER_USER_TABLES.name(),
            GrantedAuthorityName.ROLE_ADMINISTER_TABLES.name(),
            GrantedAuthorityName.ROLE_DATA_COLLECTOR.name(),
            GrantedAuthorityName.ROLE_DATA_VIEWER.name(),
            GrantedAuthorityName.ROLE_DATA_OWNER.name()
    });

    impliedAuthorities = Collections.unmodifiableMap(authoritiesMap);
  }
  
  /**
   * 
   * @param authorityName
   * @return either the canonical name for the group sent down to the device or null. 
   */
  private static String constructGroupName(String groupPrefix, String authorityName) {
    groupPrefix = groupPrefix.toUpperCase(Locale.US);
    authorityName = authorityName.toUpperCase(Locale.US);

    String prefixSpace = groupPrefix + " ";
    if ( authorityName.startsWith(prefixSpace) ) {
      String name = authorityName.substring(prefixSpace.length());
      name = "GROUP_" + name.replaceAll("[\\s\\p{Punct}]+", "_");
      return name;
    }
    return null;
  }

  /**
   * Check whether the rawName starts with the group prefix. 
   * If it does, convert it to an ODK group name (via constructGroupName(...), above)
   * and then check whether it is one of the well-known group names, and should be converted
   * to the dominant role for that group, or if it isn't, and should be returned as a simple
   * ODK group name.
   * 
   * @param groupPrefix
   * @param rawName
   * @return null or a well-known role or a group if not a well-known group name
   */
  public static String resolveAsGroupOrRoleAuthority(String groupPrefix, String rawName) {
    String name = constructGroupName(groupPrefix, rawName);
    if ( name == null ) {
      return null;
    } else if ( name.compareToIgnoreCase(GrantedAuthorityName.GROUP_DATA_COLLECTORS.name()) == 0 ) {
      return GrantedAuthorityName.ROLE_DATA_COLLECTOR.name();
    } else if ( name.compareToIgnoreCase(GrantedAuthorityName.GROUP_DATA_VIEWERS.name()) == 0 ) {
      return GrantedAuthorityName.ROLE_DATA_VIEWER.name();
    } else if ( name.compareToIgnoreCase(GrantedAuthorityName.GROUP_FORM_MANAGERS.name()) == 0 ) {
      return GrantedAuthorityName.ROLE_DATA_OWNER.name();
    } else if ( name.compareToIgnoreCase(GrantedAuthorityName.GROUP_SYNCHRONIZE_TABLES.name()) == 0 ) {
      return GrantedAuthorityName.ROLE_SYNCHRONIZE_TABLES.name();
    } else if ( name.compareToIgnoreCase(GrantedAuthorityName.GROUP_SUPER_USER_TABLES.name()) == 0 ) {
      return GrantedAuthorityName.ROLE_SUPER_USER_TABLES.name();
    } else if ( name.compareToIgnoreCase(GrantedAuthorityName.GROUP_ADMINISTER_TABLES.name()) == 0 ) {
      return GrantedAuthorityName.ROLE_ADMINISTER_TABLES.name();
    } else if ( name.compareToIgnoreCase(GrantedAuthorityName.GROUP_SITE_ADMINS.name()) == 0 ) {
      return GrantedAuthorityName.ROLE_SITE_ACCESS_ADMIN.name();
    } else {
      return name;
    }
  }

  /**
   * Check whether the rawName starts with the group prefix. 
   * If it does, convert it to an ODK group name (via constructGroupName(...), above)
   * and then check whether it is one of the well-known group names. If so, then a set 
   * of roles for that group is added to the mappedSet, or, if it isn't, then just the 
   * simple ODK group name is added to the mappedSet.
   * 
   * If the group does not start with the group prefix, no action is taken.
   * 
   * @param groupPrefix
   * @param mappedSet
   * @param rawName
   */
  public static void addGroupOrRoleAuthorities(String groupPrefix, Set<GrantedAuthority> mappedSet, String rawName) {
    String name = constructGroupName(groupPrefix, rawName);
    if (name == null) {
      return;
    } else if (name.compareToIgnoreCase(GrantedAuthorityName.GROUP_DATA_COLLECTORS.name()) == 0) {
      mappedSet.add(new SimpleGrantedAuthority(GrantedAuthorityName.ROLE_USER.name()));
      mappedSet.add(new SimpleGrantedAuthority(GrantedAuthorityName.ROLE_DATA_COLLECTOR.name()));
    } else if (name.compareToIgnoreCase(GrantedAuthorityName.GROUP_DATA_VIEWERS.name()) == 0) {
      mappedSet.add(new SimpleGrantedAuthority(GrantedAuthorityName.ROLE_USER.name()));
      mappedSet.add(new SimpleGrantedAuthority(GrantedAuthorityName.ROLE_DATA_VIEWER.name()));
    } else if (name.compareToIgnoreCase(GrantedAuthorityName.GROUP_FORM_MANAGERS.name()) == 0) {
      mappedSet.add(new SimpleGrantedAuthority(GrantedAuthorityName.ROLE_USER.name()));
      mappedSet.add(new SimpleGrantedAuthority(GrantedAuthorityName.ROLE_DATA_VIEWER.name()));
      mappedSet.add(new SimpleGrantedAuthority(GrantedAuthorityName.ROLE_DATA_OWNER.name()));
    } else if (name.compareToIgnoreCase(GrantedAuthorityName.GROUP_SYNCHRONIZE_TABLES.name()) == 0) {
      mappedSet.add(new SimpleGrantedAuthority(GrantedAuthorityName.ROLE_USER.name()));
      mappedSet.add(new SimpleGrantedAuthority(GrantedAuthorityName.ROLE_SYNCHRONIZE_TABLES.name()));
    } else if (name.compareToIgnoreCase(GrantedAuthorityName.GROUP_SUPER_USER_TABLES.name()) == 0) {
      mappedSet.add(new SimpleGrantedAuthority(GrantedAuthorityName.ROLE_USER.name()));
      mappedSet.add(new SimpleGrantedAuthority(GrantedAuthorityName.ROLE_SYNCHRONIZE_TABLES.name()));
      mappedSet.add(new SimpleGrantedAuthority(GrantedAuthorityName.ROLE_SUPER_USER_TABLES.name()));
    } else if (name.compareToIgnoreCase(GrantedAuthorityName.GROUP_ADMINISTER_TABLES.name()) == 0) {
      mappedSet.add(new SimpleGrantedAuthority(GrantedAuthorityName.ROLE_USER.name()));
      mappedSet.add(new SimpleGrantedAuthority(GrantedAuthorityName.ROLE_SYNCHRONIZE_TABLES.name()));
      mappedSet.add(new SimpleGrantedAuthority(GrantedAuthorityName.ROLE_SUPER_USER_TABLES.name()));
      mappedSet.add(new SimpleGrantedAuthority(GrantedAuthorityName.ROLE_ADMINISTER_TABLES.name()));
    } else if (name.compareToIgnoreCase(GrantedAuthorityName.GROUP_SITE_ADMINS.name()) == 0) {
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
   * Resolves the implied authorities.
   * Returns the implied authorities, including the provided authority.
   * If no authority is implied, then an empty set is returned.
   *
   * @param authority
   * @return
   */
  public static Set<GrantedAuthority> resolveImpliedRoleAuthority(String authority) {
    return Arrays
            .stream(impliedAuthorities.getOrDefault(authority, new String[] { authority }))
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toSet());
  }

  private static final class UserIdFullName {
    final String user_id;
    final String full_name;
    
    UserIdFullName(UserSecurityInfo userSecurityInfo) {
      if ( userSecurityInfo.getType() == UserType.ANONYMOUS ) {
        user_id = "anonymous"; 
        full_name = User.ANONYMOUS_USER_NICKNAME;
      } else if ( userSecurityInfo.getEmail() == null ) {
        user_id = "username:" + userSecurityInfo.getUsername(); 
        if ( userSecurityInfo.getFullName() == null ) {
          full_name = userSecurityInfo.getUsername();
        } else {
          full_name = userSecurityInfo.getFullName();
        }
      } else {
        // already has the mailto: prefix
        user_id = userSecurityInfo.getEmail(); 
        if ( userSecurityInfo.getFullName() == null ) {
          full_name = userSecurityInfo.getEmail().substring(EmailParser.K_MAILTO.length());
        } else {
          full_name = userSecurityInfo.getFullName();
        }
      }
    }
    
    UserIdFullName(User user) {
      if ( user.isAnonymous() ) {
        user_id = "anonymous";
        full_name = User.ANONYMOUS_USER_NICKNAME;
      } else if ( user.getEmail() == null ) {
        // TODO: fix this in Aggregate back-port
        user_id = "username:" + user.getUriUser();
        if ( user.getNickname() == null ) {
          full_name = user.getUriUser();
        } else {
          full_name = user.getNickname();
        }
      } else {
        user_id = user.getEmail();
        if ( user.getNickname() == null ) {
          full_name = user.getEmail().substring(EmailParser.K_MAILTO.length());
        } else {
          full_name = user.getNickname();
        }
      }

    }
  }
  private static final ArrayList<String> processRoles(TreeSet<String> grants) {
    ArrayList<String> roleNames = new ArrayList<String>();
    roleNames.addAll(grants);
    return roleNames;
  }

  /**
   * Constructor to extract content from UserSecurityInfo
   * 
   * @param userSecurityInfo
   */
  public static final UserInfo createUserInfo(UserSecurityInfo userSecurityInfo) {
    ArrayList<String> roles;
    
    UserIdFullName fields = new UserIdFullName(userSecurityInfo);
    roles = processRoles(userSecurityInfo.getGrantedAuthorities());
    
    return new UserInfo(fields.user_id, fields.full_name, roles);
  }
  
  public static PrivilegesInfo getRolesAndDefaultGroup(CallingContext cc) {
    
    User user = cc.getCurrentUser();
    Set<GrantedAuthority> grants = new HashSet<GrantedAuthority>();
    grants.addAll(user.getAuthorities());

    DirectoryAwareAuthenticationProvider provider
            = (DirectoryAwareAuthenticationProvider) cc.getBean(SecurityBeanDefs.DIRECTORY_AWARE_AUTHENTICATION_PROVIDER);
    
    String defaultGroup = provider.getDefaultGroup(cc);
    boolean matchesMembershipGroup = (defaultGroup == null);
    
    ArrayList<String> roleNames = new ArrayList<String>();
    for ( GrantedAuthority a : grants ) {
      String authName = a.getAuthority();
      roleNames.add(authName);
      matchesMembershipGroup = matchesMembershipGroup || authName.equals(defaultGroup);
    }
    Collections.sort(roleNames);
    UserIdFullName fields = new UserIdFullName(user);

    PrivilegesInfo info = new PrivilegesInfo(fields.user_id, fields.full_name,
        roleNames, (matchesMembershipGroup ? defaultGroup : null));

    return info;
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

    DirectoryAwareAuthenticationProvider provider
            = (DirectoryAwareAuthenticationProvider) cc.getBean(SecurityBeanDefs.DIRECTORY_AWARE_AUTHENTICATION_PROVIDER);
	
	return (ArrayList<UserSecurityInfo>) provider.getAllUsers(withAuthorities, cc);
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
