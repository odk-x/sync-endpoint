package org.opendatakit.common.security.spring;

import org.opendatakit.common.security.User;
import org.opendatakit.common.security.client.UserSecurityInfo;
import org.opendatakit.common.security.server.SecurityServiceUtil;
import org.opendatakit.common.web.CallingContext;
import org.springframework.ldap.NamingException;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.support.LdapEncoder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.ldap.SpringSecurityLdapTemplate;
import org.springframework.security.ldap.authentication.LdapAuthenticator;
import org.springframework.security.ldap.userdetails.InetOrgPerson;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.util.Assert;

import javax.naming.directory.SearchControls;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * LDAP Authentication Provider
 *
 * Assumes that users are stored in objectClass posixAccount
 * and groups are stored in objectClass posixGroup
 */
public class DefaultLdapAuthenticationProvider
        extends org.springframework.security.ldap.authentication.LdapAuthenticationProvider
        implements DirectoryAwareAuthenticationProvider {

    private static final String GROUP_MEMBER_ATTR = "memberUid";
    private static final String USER_DEFAULT_GROUP_ATTR = "gidNumber";

    private final SpringSecurityLdapTemplate ldapTemplate;
    private final SearchControls searchControls = new SearchControls();
    private final GroupPrefixAwareAuthoritiesMapper authoritiesMapper;

    private String groupSearchBase;
    private String userSearchBase;
    private String groupSearchFilter; // (&(objectClass=posixGroup)({1}={0} *))
    private String userFullnameAttribute = "givenName";
    private String usernameAttribute = "uid";
    private String userDnPattern;
    private String groupRoleAttribute;

    public SpringSecurityLdapTemplate getLdapTemplate() {
        return this.ldapTemplate;
    }

    public SearchControls getSearchControls() {
        return this.searchControls;
    }

    public GroupPrefixAwareAuthoritiesMapper getAuthoritiesMapper() {
        return this.authoritiesMapper;
    }

    public String getGroupSearchBase() {
        return groupSearchBase;
    }

    public void setGroupSearchBase(String groupSearchBase) {
        this.groupSearchBase = groupSearchBase;
    }

    public String getUserSearchBase() {
        return userSearchBase;
    }

    public void setUserSearchBase(String userSearchBase) {
        this.userSearchBase = userSearchBase;
    }

    public String getGroupSearchFilter() {
        return groupSearchFilter;
    }

    public void setGroupSearchFilter(String groupSearchFilter) {
        this.groupSearchFilter = groupSearchFilter;
    }

    public String getUserFullnameAttribute() {
        return userFullnameAttribute;
    }

    public void setUserFullnameAttribute(String userFullnameAttribute) {
        this.userFullnameAttribute = userFullnameAttribute;
    }

    public String getUsernameAttribute() {
        return usernameAttribute;
    }

    public void setUsernameAttribute(String usernameAttribute) {
        this.usernameAttribute = usernameAttribute;
    }

    public String getUserDnPattern() {
        return userDnPattern;
    }

    public void setUserDnPattern(String userDnPattern) {
        this.userDnPattern = userDnPattern;
    }

    public String getGroupRoleAttribute() {
        return groupRoleAttribute;
    }

    public void setGroupRoleAttribute(String groupRoleAttribute) {
        this.groupRoleAttribute = groupRoleAttribute;
    }

    public DefaultLdapAuthenticationProvider(
            ContextSource contextSource,
            LdapAuthenticator authenticator,
            LdapAuthoritiesPopulator authoritiesPopulator,
            GroupPrefixAwareAuthoritiesMapper authoritiesMapper) {
        super(authenticator, authoritiesPopulator);

        Assert.notNull(contextSource);
        Assert.notNull(authoritiesMapper);

        this.ldapTemplate = new SpringSecurityLdapTemplate(contextSource);
        ldapTemplate.setSearchControls(searchControls);

        this.authoritiesMapper = authoritiesMapper;
    }

    /**
     * Use the group defined in gidNumber as the default group
     * The gidNumber is used to find the corresponding cn
     * then resolved as a role
     *
     * When a group has multiple CNs, one of them is picked
     * No guarantee on which one
     *
     * Null is returned if any of the following is true,
     *   1. the gidNumber doesn't correspond to a group
     *   2. the group is not in the search base
     *   3. cn of the group doesn't start with correct group prefix
     *
     * @param cc
     * @return the defaultGroup of the current user or null.
     */
    @Override
    public String getDefaultGroup(CallingContext cc) {
        String uid = cc.getCurrentUser().getUriUser();

        // consider creating posixAccount class that stores gidNumber, this will save 1 ldap call
        InetOrgPerson user = (InetOrgPerson) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String gidNumber = getLdapTemplate()
                .retrieveEntry(user.getDn(), new String[] { USER_DEFAULT_GROUP_ATTR })
                .getStringAttribute(USER_DEFAULT_GROUP_ATTR);
        String groupName = getLdapTemplate()
                .retrieveEntry(USER_DEFAULT_GROUP_ATTR + "=" + gidNumber + "," + groupSearchBase, new String[] { getGroupRoleAttribute() })
                .getStringAttribute(getGroupRoleAttribute());

        try {
            String authority = SecurityServiceUtil.resolveAsGroupOrRoleAuthority(
                    getAuthoritiesMapper().getGroupPrefix(),
                    groupName
            );

            if (authority != null) {
                return authority;
            }
        } catch (NamingException e) {
            // logged below
        }

        logger.error("Error retrieving default group for user: " + uid);
        return null;
    }

    /**
     * Returns all registered users and the anonymous user
     *
     * @param withAuthorities
     * @param cc
     * @return
     */
    @Override
    public List<UserSecurityInfo> getAllUsers(final boolean withAuthorities, CallingContext cc) {
        String[] attributes = withAuthorities ?
                new String[] { GROUP_MEMBER_ATTR, getGroupRoleAttribute() } :
                new String[] { GROUP_MEMBER_ATTR };

        // get all groups under the group prefix
        // and their member list
        Set<Map<String, List<String>>> groups = getLdapTemplate()
                .searchForMultipleAttributeValues(
                        getGroupSearchBase(),
                        getGroupSearchFilter(),
                        new String[] { getAuthoritiesMapper().getGroupPrefix(), getGroupRoleAttribute() },
                        attributes
                );

        Map<String, List<String>> allUserMembership = null;
        if (withAuthorities) {
            // invert groups, convert it to memberUid -> group cn
            allUserMembership = new HashMap<>();
            for (Map<String, List<String>> group : groups) {
                for (String uid : group.get(GROUP_MEMBER_ATTR)) {
                    List<String> memberships = allUserMembership.getOrDefault(uid, new ArrayList<>());
                    memberships.add(group.get(getGroupRoleAttribute()).get(0));
                    allUserMembership.putIfAbsent(uid, memberships);
                }
            }
        }

        // if with authorities, set of uids is already stored in allUserMembership
        Set<String> memberUids;
        memberUids = withAuthorities ? allUserMembership.keySet() : groups
                .stream()
                .flatMap(g -> g.get(GROUP_MEMBER_ATTR).stream())
                .collect(Collectors.toSet());

        // retrieve userFullname for all users, in parallel
        final String userDnPattern = getUserDnPattern();
        final String userFullnameAttribute = getUserFullnameAttribute();
        final String usernameAttribute = getUsernameAttribute();
        List<UserSecurityInfo> usiList = memberUids
                .parallelStream()
                .map(uid -> MessageFormat.format(userDnPattern, LdapEncoder.filterEncode(uid)))
                .map(dn -> getLdapTemplate().retrieveEntry(dn, new String[] { usernameAttribute, userFullnameAttribute }))
                .map(user -> new UserSecurityInfo(
                        user.getStringAttribute(usernameAttribute),
                        user.getStringAttribute(userFullnameAttribute),
                        null,
                        UserSecurityInfo.UserType.REGISTERED
                ))
                .collect(Collectors.toList());

        // map authorities for all users
        if (withAuthorities) {
            for (UserSecurityInfo usi : usiList) {
                Collection<? extends GrantedAuthority> authorities = allUserMembership
                        .get(usi.getUsername())
                        .stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                authorities = getAuthoritiesMapper().mapAuthorities(authorities);
                SecurityServiceUtil.setUserAuthenticationLists(usi, authorities);
            }
        }

        UserSecurityInfo anonymous = new UserSecurityInfo(
                User.ANONYMOUS_USER,
                User.ANONYMOUS_USER_NICKNAME,
                null,
                UserSecurityInfo.UserType.ANONYMOUS
        );
        if (withAuthorities) {
            SecurityServiceUtil.setAuthenticationListsForAnonymousUser(anonymous, cc);
        }

        return usiList;
    }
}
