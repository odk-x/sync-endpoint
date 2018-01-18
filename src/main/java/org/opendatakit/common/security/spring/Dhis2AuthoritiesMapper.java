package org.opendatakit.common.security.spring;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public class Dhis2AuthoritiesMapper extends PrefixedAuthoritiesMapper {
  private final Map<String, String> authoritiesMapping;

  private Map<String, String> getAuthoritiesMapping() {
    return authoritiesMapping;
  }

  public Dhis2AuthoritiesMapper(String[] defaultAuthorities, Map<String, String> authoritiesMapping) {
    super("", defaultAuthorities);

    this.authoritiesMapping = authoritiesMapping;
  }

  @Override
  public Collection<? extends GrantedAuthority> mapAuthorities(Collection<? extends GrantedAuthority> authorities) {
    return super.mapAuthorities(authorities
        .stream()
        .map(GrantedAuthority::getAuthority)
        .map(auth -> getAuthoritiesMapping().getOrDefault(auth, auth))
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toSet())
    );
  }
}
