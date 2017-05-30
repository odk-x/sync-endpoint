package org.opendatakit.common.security.spring;

import java.util.Collection;
import java.util.HashSet;
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
  public Collection<? extends GrantedAuthority> mapAuthorities(
      Collection<? extends GrantedAuthority> base) {
    Set<GrantedAuthority> mappedSet = new HashSet<GrantedAuthority>();
    for (GrantedAuthority ga : base) {
      SecurityServiceUtil.addGroupOrRoleAuthorities(getGroupPrefix(), mappedSet, ga.getAuthority());
    }
    if (!mappedSet.isEmpty()) {
      mappedSet.add(new SimpleGrantedAuthority(GrantedAuthorityName.AUTH_ACTIVE_DIRECTORY.name()));
      mappedSet.add(new SimpleGrantedAuthority(GrantedAuthorityName.USER_IS_REGISTERED.name()));
      mappedSet.add(new SimpleGrantedAuthority(GrantedAuthorityName.ROLE_USER.name()));
    }
    return mappedSet;
  }

}
