package org.opendatakit.common.security.spring;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.opendatakit.common.security.common.GrantedAuthorityName;
import org.opendatakit.common.security.server.SecurityServiceUtil;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;

public class ActiveDirectoryAuthoritiesMapper implements GrantedAuthoritiesMapper {

	private final String groupPrefix;
	
	public ActiveDirectoryAuthoritiesMapper(String groupPrefix) {
		this.groupPrefix = groupPrefix;
	}
	
	public String getGroupPrefix() {
		return groupPrefix;
	}
	
	@Override
	public Collection<? extends GrantedAuthority> mapAuthorities(Collection<? extends GrantedAuthority> base) {
		Set<GrantedAuthority> mappedSet = new HashSet<GrantedAuthority>();
		String prefixSpace = groupPrefix + " ";
		for ( GrantedAuthority ga : base ) {
			if ( ga.getAuthority().startsWith(prefixSpace) ) {
				String name = ga.getAuthority().substring(prefixSpace.length()).toUpperCase(Locale.US);
				name = "GROUP_" + name.replaceAll("[\\s\\p{Punct}]+", "_");
				SecurityServiceUtil.addGroupOrRoleAuthorities(mappedSet, name);
			}
		}
		if ( !mappedSet.isEmpty() ) {
			mappedSet.add(new SimpleGrantedAuthority(GrantedAuthorityName.AUTH_ACTIVE_DIRECTORY.name()));
			mappedSet.add(new SimpleGrantedAuthority(GrantedAuthorityName.USER_IS_REGISTERED.name()));
			mappedSet.add(new SimpleGrantedAuthority(GrantedAuthorityName.ROLE_USER.name()));
		}
		return mappedSet;
	}

}
