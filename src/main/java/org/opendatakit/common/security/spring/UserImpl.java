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

import org.opendatakit.common.security.User;
import org.opendatakit.common.security.common.GrantedAuthorityName;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class UserImpl implements org.opendatakit.common.security.User {

	final String nickName;
	final String email;
	final String uriUser;
	final Set<GrantedAuthority> authorities = new HashSet<GrantedAuthority>();
	
	UserImpl(String uriUser, String email, String nickName,
			Collection<? extends GrantedAuthority> groupsAndGrantedAuthorities) {
		this.uriUser = uriUser;
		this.email = email;
		this.nickName = nickName;
		this.authorities.addAll(groupsAndGrantedAuthorities);
	}
	
	@Override
	public String getNickname() {
		return nickName;
	}

	@Override
	public String getEmail() {
		return email;
	}
	
	public Set<GrantedAuthority> getAuthorities() {
		return Collections.unmodifiableSet(authorities);
	}

	@Override
	public String getUriUser() {
		return uriUser;
	}

	@Override
	public boolean isAnonymous() {
		return uriUser.equals(User.ANONYMOUS_USER);
	}
	
	@Override
	public boolean isRegistered() {
		return authorities.contains(new SimpleGrantedAuthority(GrantedAuthorityName.USER_IS_REGISTERED.name()));
	}

	@Override
	public boolean equals(Object obj) {
		if ( obj == null || !(obj instanceof org.opendatakit.common.security.User)) {
			return false;
		}
		
		org.opendatakit.common.security.User u = (org.opendatakit.common.security.User) obj;
		return u.getUriUser().equals(getUriUser());
	}

	@Override
	public int hashCode() {
		return getUriUser().hashCode();
	}
}
