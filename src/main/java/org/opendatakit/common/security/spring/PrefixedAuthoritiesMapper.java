package org.opendatakit.common.security.spring;

import org.opendatakit.common.security.server.SecurityServiceUtil;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class PrefixedAuthoritiesMapper implements GroupPrefixAwareAuthoritiesMapper {
    private final String groupPrefix;
    private final String[] defaultAuthorities;

    @Override
    public String getGroupPrefix() {
        return this.groupPrefix;
    }

    public String[] getDefaultAuthorities() {
        return this.defaultAuthorities;
    }

    public PrefixedAuthoritiesMapper(String groupPrefix, String[] defaultAuthorities) {
        Assert.hasText(groupPrefix);

        this.groupPrefix = groupPrefix;
        this.defaultAuthorities = defaultAuthorities;
    }

    /**
     * Maps authorities using SecurityServiceUtil
     * And resolves implied authorities using SecurityServiceUtil
     *
     * @param authorities
     * @return
     */
    @Override
    public Collection<? extends GrantedAuthority> mapAuthorities(Collection<? extends GrantedAuthority> authorities) {
        final String groupPrefix = getGroupPrefix();

        Set<GrantedAuthority> mappedAuthorities = authorities
                .stream()
                .map(GrantedAuthority::getAuthority)
                .map(rawName -> SecurityServiceUtil.resolveAsGroupOrRoleAuthority(groupPrefix, rawName))
                .filter(Objects::nonNull) // nonNull to filter out invalid authorities
                .map(SecurityServiceUtil::resolveImpliedRoleAuthority)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        if (!mappedAuthorities.isEmpty()) {
            mappedAuthorities.addAll(
                    Arrays.stream(getDefaultAuthorities())
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList())
            );
        }

        return mappedAuthorities;
    }
}
