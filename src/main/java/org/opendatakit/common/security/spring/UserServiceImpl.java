/*
 * Copyright (C) 2010 University of Washington
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
package org.opendatakit.common.security.spring;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.opendatakit.common.security.Realm;
import org.opendatakit.common.security.SecurityUtils;
import org.opendatakit.common.security.User;
import org.opendatakit.common.security.common.GrantedAuthorityName;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public class UserServiceImpl implements org.opendatakit.common.security.UserService,
    InitializingBean {

  // configured by bean definition...
  Realm realm;

  User lastUser = null;

  public UserServiceImpl() {
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    if (realm == null) {
      throw new IllegalStateException("realm must be configured");
    }
  }

  public Realm getRealm() {
    return realm;
  }

  public void setRealm(Realm realm) {
    this.realm = realm;
  }

  @Override
  public String createLoginURL() {
    return "login.html";
  }

  @Override
  public String createLogoutURL() {
    return "j_spring_security_logout";
  }

  @Override
  public Realm getCurrentRealm() {
    return realm;
  }

  private boolean isAnonymousUser(Authentication auth) {
    if (auth == null) {
      throw new NullPointerException("Unexpected null pointer from authentication retrieval");
    } else if (!auth.isAuthenticated()) {
      throw new IllegalStateException(
          "Unexpected unauthenticated user from authentication retrieval (expect anonymous authentication)");
    } else if ((auth.getPrincipal() instanceof String)
        && ((String) auth.getPrincipal()).equals("anonymousUser")) {
      return true;
    } else {
      return false;
    }
  }

  private synchronized User internalGetUser(String uriUser,
      Collection<? extends GrantedAuthority> authorities) {
    User match = lastUser;
    if ( match != null && match.getUriUser().equals(uriUser) ) {
      return match;
    }
    
    if (User.ANONYMOUS_USER.equals(uriUser)) {
      // ignored passed-in authorities
      match = new UserImpl(User.ANONYMOUS_USER, null, User.ANONYMOUS_USER_NICKNAME, authorities);
      lastUser = match;
      return match;
    } else if (User.DAEMON_USER.equals(uriUser)) {
      // ignored passed-in authorities
      Set<GrantedAuthority> daemonGroups = new HashSet<GrantedAuthority>();
      daemonGroups = new HashSet<GrantedAuthority>();
      daemonGroups.add(new SimpleGrantedAuthority(GrantedAuthorityName.USER_IS_DAEMON.name()));
      match = new UserImpl(User.DAEMON_USER, null, User.DAEMON_USER_NICKNAME, daemonGroups);
      return match;
    } else {
      match = new UserImpl(uriUser, getEmail(uriUser, null), getNickname(uriUser), authorities);
      lastUser = match;
      return match;
    }
  }

  @Override
  public User getCurrentUser() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    return internalGetUser(auth.getName(), auth.getAuthorities());
  }

  @Override
  public boolean isUserLoggedIn() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return !isAnonymousUser(auth);
  }

  @Override
  public User getDaemonAccountUser() {
    return internalGetUser(User.DAEMON_USER, null);
  }

  private static final String getNickname(String uriUser) {
    String name = uriUser;
    if (name.startsWith(SecurityUtils.MAILTO_COLON)) {
      name = name.substring(SecurityUtils.MAILTO_COLON.length());
      int idxTimestamp = name.indexOf("|");
      if (idxTimestamp != -1) {
        name = name.substring(0, idxTimestamp);
      }
    } else if (name.startsWith(SecurityUtils.UID_PREFIX)) {
      name = name.substring(SecurityUtils.UID_PREFIX.length());
      int idxTimestamp = name.indexOf("|");
      if (idxTimestamp != -1) {
        name = name.substring(0, idxTimestamp);
      }
    }
    return name;
  }

  private static final String getEmail(String uriUser, String oauth2Email) {
    if (oauth2Email != null) {
      return oauth2Email;
    }
    if (uriUser.startsWith(SecurityUtils.MAILTO_COLON)) {
      String n = uriUser;
      int idxTimestamp = n.indexOf("|");
      if (idxTimestamp != -1) {
        return n.substring(0, idxTimestamp);
      }
      return n;
    }
    return null;
  }
}
