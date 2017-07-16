package org.opendatakit.common.security.spring;

import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;

/**
 * GrantedAuthoritiesMapper that checks group prefix
 */
public interface GroupPrefixAwareAuthoritiesMapper extends GrantedAuthoritiesMapper {
    /**
     *
     * @return the default group
     */
    String getGroupPrefix();
}
