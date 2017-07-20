/*
 * Copyright 2002-2016 the original author or authors.
 * Copyright 2017 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opendatakit.common.security.spring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;
import javax.naming.PartialResultException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapName;

import org.opendatakit.common.persistence.client.exception.DatastoreFailureException;
import org.opendatakit.common.security.User;
import org.opendatakit.common.security.client.UserSecurityInfo;
import org.opendatakit.common.security.client.exception.AccessDeniedException;
import org.opendatakit.common.security.server.SecurityServiceUtil;
import org.opendatakit.common.web.CallingContext;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.ldap.InvalidNameException;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.support.DefaultDirObjectFactory;
import org.springframework.ldap.support.LdapUtils;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.ldap.SpringSecurityLdapTemplate;
import org.springframework.security.ldap.authentication.AbstractLdapAuthenticationProvider;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Specialized LDAP authentication provider which uses Active Directory configuration
 * conventions.
 * <p>
 * It will authenticate using the Active Directory <a
 * href="http://msdn.microsoft.com/en-us/library/ms680857%28VS.85%29.aspx">
 * {@code userPrincipalName}</a> or a custom {@link #setSearchFilter(String) searchFilter}
 * in the form {@code username@domain}. If the username does not already end with the
 * domain name, the {@code userPrincipalName} will be built by appending the configured
 * domain name to the username supplied in the authentication request. If no domain name
 * is configured, it is assumed that the username will always contain the domain name.
 * <p>
 * The user authorities are obtained from the data contained in the {@code memberOf}
 * attribute.
 *
 * <h3>Active Directory Sub-Error Codes</h3>
 *
 * When an authentication fails, resulting in a standard LDAP 49 error code, Active
 * Directory also supplies its own sub-error codes within the error message. These will be
 * used to provide additional log information on why an authentication has failed. Typical
 * examples are
 *
 * <ul>
 * <li>525 - user not found</li>
 * <li>52e - invalid credentials</li>
 * <li>530 - not permitted to logon at this time</li>
 * <li>532 - password expired</li>
 * <li>533 - account disabled</li>
 * <li>701 - account expired</li>
 * <li>773 - user must reset password</li>
 * <li>775 - account locked</li>
 * </ul>
 *
 * If you set the {@link #setConvertSubErrorCodesToExceptions(boolean)
 * convertSubErrorCodesToExceptions} property to {@code true}, the codes will also be used
 * to control the exception raised.
 *
 * @author Luke Taylor
 * @author Rob Winch
 * @since 3.1
 */
public final class ActiveDirectoryLdapAuthenticationProvider
		extends AbstractLdapAuthenticationProvider
		implements DirectoryAwareAuthenticationProvider {
	private static final Pattern SUB_ERROR_CODE = Pattern
			.compile(".*data\\s([0-9a-f]{3,4}).*");

	// Error codes
	private static final int USERNAME_NOT_FOUND = 0x525;
	private static final int INVALID_PASSWORD = 0x52e;
	private static final int NOT_PERMITTED = 0x530;
	private static final int PASSWORD_EXPIRED = 0x532;
	private static final int ACCOUNT_DISABLED = 0x533;
	private static final int ACCOUNT_EXPIRED = 0x701;
	private static final int PASSWORD_NEEDS_RESET = 0x773;
	private static final int ACCOUNT_LOCKED = 0x775;

	private final String domain;
	private final String rootDn;
	private final String url;
	private String ldapUser;
	private String ldapPassword;
	private boolean convertSubErrorCodesToExceptions;
	private String searchFilter = "(&(objectClass=user)(userPrincipalName={0}))";

	// Only used to allow tests to substitute a mock LdapContext
	ContextFactory contextFactory = new ContextFactory();

	/**
	 * @param domain the domain name (may be null or empty)
	 * @param url an LDAP url (or multiple URLs)
	 * @param rootDn the root DN (may be null or empty)
	 */
	public ActiveDirectoryLdapAuthenticationProvider(String domain, String url,
			String rootDn) {
		Assert.isTrue(StringUtils.hasText(url), "Url cannot be empty");
		this.domain = StringUtils.hasText(domain) ? domain.toLowerCase() : null;
		this.url = url;
		this.rootDn = StringUtils.hasText(rootDn) ? rootDn.toLowerCase() : null;
	}

	/**
	 * @param domain the domain name (may be null or empty)
	 * @param url an LDAP url (or multiple URLs)
	 */
	public ActiveDirectoryLdapAuthenticationProvider(String domain, String url) {
		Assert.isTrue(StringUtils.hasText(url), "Url cannot be empty");
		this.domain = StringUtils.hasText(domain) ? domain.toLowerCase() : null;
		this.url = url;
		rootDn = this.domain == null ? null : rootDnFromDomain(this.domain);
	}

	public String getLdapUser() {
		return ldapUser;
	}

	public void setLdapUser(String ldapUser) {
		this.ldapUser = ldapUser;
	}

	public String getLdapPassword() {
		return ldapPassword;
	}

	public void setLdapPassword(String ldapPassword) {
		this.ldapPassword = ldapPassword;
	}

	private PrefixedAuthoritiesMapper savedAuthoritiesMapper;
	
	public void setAuthoritiesMapper(PrefixedAuthoritiesMapper authoritiesMapper) {
		Assert.notNull(authoritiesMapper);

		this.savedAuthoritiesMapper = authoritiesMapper;
		super.setAuthoritiesMapper(authoritiesMapper);
	}
	
	public PrefixedAuthoritiesMapper getGrantedAuthoritiesMapper() {
		return savedAuthoritiesMapper;
	}
	
	public String getUrl() {
		return url;
	}

	public String getRootDn() {
		return rootDn;
	}

	public String getSearchFilter() {
		return searchFilter;
	}
	
	/**
	 * @param cc
	 * @return the defaultGroup of the current user or null.
	 *         This is the defaultGroup from the LDAP system
	 *         (on ActiveDirectory, it is found in streetAddress)
	 *         This may not be a group in which the user belongs.
	 *         The caller is responsible for filtering out those
	 *         before making use of this value.
	 */
	@Override
	public String getDefaultGroup(CallingContext cc) {
      final String username = getLdapUser();
      final String password = getLdapPassword();

      DirContext context = bindAsUser(username, password);

      try {
        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);

        String uri = cc.getCurrentUser().getUriUser();
        String bindPrincipal = createBindPrincipal(uri);
        String searchRoot = rootDn != null ? rootDn
              : searchRootFromPrincipal(bindPrincipal);

        DirContextOperations userRecord = 
            SpringSecurityLdapTemplate.searchForSingleEntryInternal(context,
                 searchControls, searchRoot, searchFilter,
                 new Object[] { bindPrincipal });

        Assert.notNull(userRecord,
            "No object returned by search, DirContext is not correctly configured");
        
        String[] cnDefaultGroupList = userRecord.getStringAttributes("streetAddress");
        if (cnDefaultGroupList == null || cnDefaultGroupList.length == 0) {
          return null;
        }
        
        String cnDefaultGroup = cnDefaultGroupList[0];
        
        LdapName name = LdapUtils.newLdapName(cnDefaultGroup);
        
        try {
           // see if the group exists
           context.getAttributes(name, new String[]{"memberOf"});
        } catch (NamingException e) {
           logger.error("Failed to locate directory entry for group: " + cnDefaultGroup, e);
           return null;
        }

        String rawGroupName = name.getRdn(name.getRdns().size() - 1).getValue().toString();

        String groupPrefix = getGrantedAuthoritiesMapper().getGroupPrefix();
        
        return SecurityServiceUtil.resolveAsGroupOrRoleAuthority(groupPrefix, rawGroupName);

      } catch (NamingException e) {
        logger.error("Error retrieving default group (streetAddress) for user: " + username, e);
        return null;
      } catch (InvalidNameException e) {
        logger.error("Error retrieving default group (streetAddress) for user: " + username, e);
        return null;
      } finally {
        LdapUtils.closeContext(context);
      }
	}
	
  /**
   * Return all registered users and the Anonymous user.
   * Invoked from SecurityServiceUtils
   *
   * @param withAuthorities
   * @param cc
   * @return
   * @throws AccessDeniedException
   * @throws DatastoreFailureException
   */
  @Override
  public ArrayList<UserSecurityInfo> getAllUsers(boolean withAuthorities, CallingContext cc)
      throws AccessDeniedException, DatastoreFailureException {

    ArrayList<DirContextOperations> results = new ArrayList<DirContextOperations>();
    ArrayList<UserSecurityInfo> users = new ArrayList<UserSecurityInfo>();

    final String username = getLdapUser();
    final String password = getLdapPassword();

    DirContext context = bindAsUser(username, password);

    try {
      SearchControls searchControls = new SearchControls();
      searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
      String searchFilter = "(&(objectClass=user)(objectClass=organizationalPerson))";
      boolean RETURN_OBJECT = true;

      SearchControls wrappedControls = new SearchControls(searchControls.getSearchScope(),
          searchControls.getCountLimit(), searchControls.getTimeLimit(),
          searchControls.getReturningAttributes(), RETURN_OBJECT,
          searchControls.getDerefLinkFlag());

      // final LdapName ctxBaseDn =
      // LdapUtils.newLdapName(context.getNameInNamespace());
      final LdapName searchBaseDn = LdapUtils.newLdapName(getRootDn());
      final NamingEnumeration<SearchResult> resultsEnum = context.search(searchBaseDn, searchFilter,
          null, wrappedControls);

      try {
        while (resultsEnum.hasMore()) {
          SearchResult searchResult = resultsEnum.next();
          DirContextAdapter dca = (DirContextAdapter) searchResult.getObject();
          Assert.notNull(dca,
              "No object returned by search, DirContext is not correctly configured");
          // don't care about users that don't have principal names
          String[] userPrincipalNameList = dca.getStringAttributes("userPrincipalName");
          if (userPrincipalNameList == null || userPrincipalNameList.length == 0) {
            continue;
          }
          String[] nameList = dca.getStringAttributes("name");
          UserSecurityInfo i = new UserSecurityInfo(userPrincipalNameList[0], nameList[0], null,
              UserSecurityInfo.UserType.REGISTERED);
          results.add(dca);
          users.add(i);
        }
      } catch (PartialResultException e) {
        try {
			resultsEnum.close();
		} catch (NamingException ex) {
          ex.printStackTrace();
        }
      }
      if (withAuthorities) {
        // process results
        for (int j = 0; j < results.size(); ++j) {
          DirContextOperations userData = results.get(j);

          String[] groups = userData.getStringAttributes("memberOf");

          if (groups == null) {
            continue;
          }
          // groups[] raw list of groups. Each of which might contain
          // sub-groups.
          // copy these into a Set and then populate a new set with all groups
          // as we
          // process them for sub-groups.
          ArrayList<String> groupsToProcess = new ArrayList<String>();
          groupsToProcess.addAll(Arrays.asList(groups));
          HashSet<String> groupsAlreadyProcessed = new HashSet<String>();

          while (!groupsToProcess.isEmpty()) {
            String group = groupsToProcess.remove(groupsToProcess.size() - 1);
            if (groupsAlreadyProcessed.contains(group)) {
              continue;
            }
            groupsAlreadyProcessed.add(group);
            try {
              Attributes attrs = context.getAttributes(LdapUtils.newLdapName(group),
                  new String[] { "memberOf" });
              Attribute includedGroups = attrs.get("memberOf");
              if (includedGroups != null) {
                for (int i = 0; i < includedGroups.size(); ++i) {
                  String includedGroup = (String) includedGroups.get(i);
                  if (!groupsAlreadyProcessed.contains(includedGroup)) {
                    groupsToProcess.add(includedGroup);
                  }
                }
              }
            } catch (NamingException e) {
              e.printStackTrace();
            }
          }

          ArrayList<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>(
              groupsAlreadyProcessed.size());

          for (String group : groupsAlreadyProcessed) {
            LdapName name = LdapUtils.newLdapName(group);
            String groupName = name.getRdn(name.getRdns().size() - 1).getValue().toString();
            authorities.add(new SimpleGrantedAuthority(groupName));
          }
          Collection<? extends GrantedAuthority> directAuthorities = getGrantedAuthoritiesMapper()
              .mapAuthorities(authorities);

          SecurityServiceUtil.setUserAuthenticationLists(users.get(j), directAuthorities);
        }
      }
    } catch (NamingException e) {
      e.printStackTrace();
      throw new DatastoreFailureException(e);
    } finally {
      LdapUtils.closeContext(context);
    }
    ArrayList<UserSecurityInfo> filteredUsers = new ArrayList<UserSecurityInfo>();
    if (withAuthorities) {
      for (UserSecurityInfo ui : users) {
        if (ui.getGrantedAuthorities().isEmpty()) {
          continue;
        }
        filteredUsers.add(ui);
      }
    } else {
      filteredUsers.addAll(users);
    }

    UserSecurityInfo anonymous = new UserSecurityInfo(User.ANONYMOUS_USER,
        User.ANONYMOUS_USER_NICKNAME, null, UserSecurityInfo.UserType.ANONYMOUS);
    if (withAuthorities) {
      SecurityServiceUtil.setAuthenticationListsForAnonymousUser(anonymous, cc);
    }
    filteredUsers.add(anonymous);
    return filteredUsers;
  }

	@Override
	protected DirContextOperations doAuthentication(
			UsernamePasswordAuthenticationToken auth) {
		String username = auth.getName();
		String password = (String) auth.getCredentials();

		DirContext ctx = bindAsUser(username, password);

		try {
			return searchForUser(ctx, username);
		}
		catch (NamingException e) {
			logger.error("Failed to locate directory entry for authenticated user: "
					+ username, e);
			throw badCredentials(e);
		}
		finally {
			LdapUtils.closeContext(ctx);
		}
	}

	/**
	 * Creates the user authority list from the values of the {@code memberOf} attribute
	 * obtained from the user's Active Directory entry.
	 */
	@Override
	protected Collection<? extends GrantedAuthority> loadUserAuthorities(
			DirContextOperations userData, String username, String password) {
		String[] groups = userData.getStringAttributes("memberOf");

		if (groups == null) {
			logger.debug("No values for 'memberOf' attribute.");

			return AuthorityUtils.NO_AUTHORITIES;
		}

		if (logger.isDebugEnabled()) {
			logger.debug("'memberOf' attribute values: " + Arrays.asList(groups));
		}

		// groups[] raw list of groups. Each of which might contain sub-groups.
		// copy these into a Set and then populate a new set with all groups as we
		// process them for sub-groups.
		ArrayList<String> groupsToProcess = new ArrayList<String>();
		groupsToProcess.addAll(Arrays.asList(groups));
		HashSet<String> groupsAlreadyProcessed = new HashSet<String>();

		DirContext ctx = bindAsUser(username, password);

		try {
			while ( !groupsToProcess.isEmpty() ) {
				String group = groupsToProcess.remove(groupsToProcess.size()-1);
				if ( groupsAlreadyProcessed.contains(group) ) {
					continue;
				}
				groupsAlreadyProcessed.add(group);
				try {
					Attributes attrs = ctx.getAttributes(LdapUtils.newLdapName(group), new String[]{"memberOf"});
					Attribute includedGroups = attrs.get("memberOf");
					if ( includedGroups != null ) {
						for ( int i = 0 ; i < includedGroups.size() ; ++i ) {
							String includedGroup = (String) includedGroups.get(i);
							if ( !groupsAlreadyProcessed.contains(includedGroup) ) {
								groupsToProcess.add(includedGroup);
							}
						}
					}
				} catch (NamingException e) {
					logger.error("Failed to locate directory entry for group: " + group, e);
				}
			}
		}
		finally {
			LdapUtils.closeContext(ctx);
		}
		
		ArrayList<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>(
				groupsAlreadyProcessed.size());

		for (String group : groupsAlreadyProcessed) {
			LdapName name = LdapUtils.newLdapName(group);
			String groupName = name.getRdn(name.getRdns().size()-1).getValue().toString();
			authorities.add(new SimpleGrantedAuthority(groupName));
		}

		return authorities;
	}

	public DirContext bindAsUser(String username, String password) {
		// TODO. add DNS lookup based on domain
		final String bindUrl = url;

		Hashtable<String, String> env = new Hashtable<String, String>();
		env.put(Context.SECURITY_AUTHENTICATION, "simple");
		String bindPrincipal = createBindPrincipal(username);
		env.put(Context.SECURITY_PRINCIPAL, bindPrincipal);
		env.put(Context.PROVIDER_URL, bindUrl);
		env.put(Context.SECURITY_CREDENTIALS, password);
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.OBJECT_FACTORIES, DefaultDirObjectFactory.class.getName());

		try {
			return contextFactory.createContext(env);
		}
		catch (NamingException e) {
			if ((e instanceof AuthenticationException)
					|| (e instanceof OperationNotSupportedException)) {
				handleBindException(bindPrincipal, e);
				throw badCredentials(e);
			}
			else {
				throw LdapUtils.convertLdapException(e);
			}
		}
	}

	private void handleBindException(String bindPrincipal, NamingException exception) {
		if (logger.isDebugEnabled()) {
			logger.debug("Authentication for " + bindPrincipal + " failed:" + exception);
		}

		int subErrorCode = parseSubErrorCode(exception.getMessage());

		if (subErrorCode <= 0) {
			logger.debug("Failed to locate AD-specific sub-error code in message");
			return;
		}

		logger.info("Active Directory authentication failed: "
				+ subCodeToLogMessage(subErrorCode));

		if (convertSubErrorCodesToExceptions) {
			raiseExceptionForErrorCode(subErrorCode, exception);
		}
	}

	private int parseSubErrorCode(String message) {
		Matcher m = SUB_ERROR_CODE.matcher(message);

		if (m.matches()) {
			return Integer.parseInt(m.group(1), 16);
		}

		return -1;
	}

	private void raiseExceptionForErrorCode(int code, NamingException exception) {
		String hexString = Integer.toHexString(code);
		Throwable cause = new ActiveDirectoryAuthenticationException(hexString,
				exception.getMessage(), exception);
		switch (code) {
		case PASSWORD_EXPIRED:
			throw new CredentialsExpiredException(messages.getMessage(
					"LdapAuthenticationProvider.credentialsExpired",
					"User credentials have expired"), cause);
		case ACCOUNT_DISABLED:
			throw new DisabledException(messages.getMessage(
					"LdapAuthenticationProvider.disabled", "User is disabled"), cause);
		case ACCOUNT_EXPIRED:
			throw new AccountExpiredException(messages.getMessage(
					"LdapAuthenticationProvider.expired", "User account has expired"),
					cause);
		case ACCOUNT_LOCKED:
			throw new LockedException(messages.getMessage(
					"LdapAuthenticationProvider.locked", "User account is locked"), cause);
		default:
			throw badCredentials(cause);
		}
	}

	private String subCodeToLogMessage(int code) {
		switch (code) {
		case USERNAME_NOT_FOUND:
			return "User was not found in directory";
		case INVALID_PASSWORD:
			return "Supplied password was invalid";
		case NOT_PERMITTED:
			return "User not permitted to logon at this time";
		case PASSWORD_EXPIRED:
			return "Password has expired";
		case ACCOUNT_DISABLED:
			return "Account is disabled";
		case ACCOUNT_EXPIRED:
			return "Account expired";
		case PASSWORD_NEEDS_RESET:
			return "User must reset password";
		case ACCOUNT_LOCKED:
			return "Account locked";
		}

		return "Unknown (error code " + Integer.toHexString(code) + ")";
	}

	private BadCredentialsException badCredentials() {
		return new BadCredentialsException(messages.getMessage(
				"LdapAuthenticationProvider.badCredentials", "Bad credentials"));
	}

	private BadCredentialsException badCredentials(Throwable cause) {
		return (BadCredentialsException) badCredentials().initCause(cause);
	}

	private DirContextOperations searchForUser(DirContext context, String username)
			throws NamingException {
		SearchControls searchControls = new SearchControls();
		searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);

		String bindPrincipal = createBindPrincipal(username);
		String searchRoot = rootDn != null ? rootDn
				: searchRootFromPrincipal(bindPrincipal);

		try {
			return SpringSecurityLdapTemplate.searchForSingleEntryInternal(context,
					searchControls, searchRoot, searchFilter,
					new Object[] { bindPrincipal });
		}
		catch (IncorrectResultSizeDataAccessException incorrectResults) {
			// Search should never return multiple results if properly configured - just
			// rethrow
			if (incorrectResults.getActualSize() != 0) {
				throw incorrectResults;
			}
			// If we found no results, then the username/password did not match
			UsernameNotFoundException userNameNotFoundException = new UsernameNotFoundException(
					"User " + username + " not found in directory.", incorrectResults);
			throw badCredentials(userNameNotFoundException);
		}
	}

	private String searchRootFromPrincipal(String bindPrincipal) {
		int atChar = bindPrincipal.lastIndexOf('@');

		if (atChar < 0) {
			logger.debug("User principal '" + bindPrincipal
					+ "' does not contain the domain, and no domain has been configured");
			throw badCredentials();
		}

		return rootDnFromDomain(bindPrincipal.substring(atChar + 1,
				bindPrincipal.length()));
	}

	private String rootDnFromDomain(String domain) {
		String[] tokens = StringUtils.tokenizeToStringArray(domain, ".");
		StringBuilder root = new StringBuilder();

		for (String token : tokens) {
			if (root.length() > 0) {
				root.append(',');
			}
			root.append("dc=").append(token);
		}

		return root.toString();
	}

	String createBindPrincipal(String username) {
		if (domain == null || username.toLowerCase().endsWith(domain)) {
			return username;
		}

		return username + "@" + domain;
	}

	/**
	 * By default, a failed authentication (LDAP error 49) will result in a
	 * {@code BadCredentialsException}.
	 * <p>
	 * If this property is set to {@code true}, the exception message from a failed bind
	 * attempt will be parsed for the AD-specific error code and a
	 * {@link CredentialsExpiredException}, {@link DisabledException},
	 * {@link AccountExpiredException} or {@link LockedException} will be thrown for the
	 * corresponding codes. All other codes will result in the default
	 * {@code BadCredentialsException}.
	 *
	 * @param convertSubErrorCodesToExceptions {@code true} to raise an exception based on
	 * the AD error code.
	 */
	public void setConvertSubErrorCodesToExceptions(
			boolean convertSubErrorCodesToExceptions) {
		this.convertSubErrorCodesToExceptions = convertSubErrorCodesToExceptions;
	}

	/**
	 * The LDAP filter string to search for the user being authenticated. Occurrences of
	 * {0} are replaced with the {@code username@domain}.
	 * <p>
	 * Defaults to: {@code (&(objectClass=user)(userPrincipalName= 0}))}
	 * </p>
	 *
	 * @param searchFilter the filter string
	 *
	 * @since 3.2.6
	 */
	public void setSearchFilter(String searchFilter) {
		Assert.hasText(searchFilter, "searchFilter must have text");
		this.searchFilter = searchFilter;
	}

	static class ContextFactory {
		DirContext createContext(Hashtable<?, ?> env) throws NamingException {
			return new InitialLdapContext(env, null);
		}
	}
}

