package org.opendatakit.common.security.spring;

import org.opendatakit.common.persistence.client.exception.DatastoreFailureException;
import org.opendatakit.common.security.User;
import org.opendatakit.common.security.client.UserSecurityInfo;
import org.opendatakit.common.security.client.exception.AccessDeniedException;
import org.opendatakit.common.security.server.SecurityServiceUtil;
import org.opendatakit.common.security.spring.dhis2.*;
import org.opendatakit.common.web.CallingContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.util.Assert;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Dhis2AuthenticationProvider implements DirectoryAwareAuthenticationProvider {
  private static final String ME_ENDPOINT_URL = "/me";
  private static final String USERS_ENDPOINT_URL = "/users?paging=false&fields=:all";
  private static final String USER_OU_ENDPOINT_URL = "/organisationUnits?withinUserHierarchy=true&paging=false";
  private static final String OU_DETAIL_ENDPOINT_URL = "/organisationUnits/{id}?includeDescendants=true";
  private static final String GROUPS_ENDPOINT_URL = "/userGroups?paging=false";

  private final String apiUrl;
  private final GrantedAuthoritiesMapper authoritiesMapper;
  private final RestTemplate adminRestTemplate;

  public Dhis2AuthenticationProvider(String apiUrl,
                                     GrantedAuthoritiesMapper authoritiesMapper,
                                     RestTemplate adminRestTemplate) {
    this.apiUrl = apiUrl;
    this.authoritiesMapper = authoritiesMapper;
    this.adminRestTemplate = adminRestTemplate;
  }

  private static UserSecurityInfo fromDhis2User(Dhis2User user) {
    return new UserSecurityInfo(user.getUserCredentials().getUsername(), null, null, UserSecurityInfo.UserType.AUTHENTICATED);
  }

  public String getApiUrl() {
    return apiUrl;
  }

  public GrantedAuthoritiesMapper getAuthoritiesMapper() {
    return authoritiesMapper;
  }

  public RestTemplate getAdminRestTemplate() {
    return adminRestTemplate;
  }

  @Override
  public String getDefaultGroup(CallingContext cc) {
    return "";
  }

  @Override
  public List<UserSecurityInfo> getAllUsers(boolean withAuthorities, CallingContext cc) {
    Stream<Dhis2User> users = getAdminRestTemplate()
        .getForEntity(getApiUrl() + USERS_ENDPOINT_URL, Dhis2UserList.class)
        .getBody()
        .getUsers()
        .stream();

    List<UserSecurityInfo> userSecurityInfoList;
    if (!withAuthorities) {
      userSecurityInfoList = users
          .map(Dhis2AuthenticationProvider::fromDhis2User)
          .collect(Collectors.toList());
    } else {
      userSecurityInfoList = users
          .map(user -> {
            UserSecurityInfo usi = fromDhis2User(user);
            SecurityServiceUtil.setUserAuthenticationLists(
                usi,
                Stream
                    .concat(
                        // fetch OUs for this user, then fetch descendants of each OU
                        // unlike the endpoint for the current user,
                        // this one cannot directly fetch all descendants of all OUs
                        getAdminRestTemplate()
                            .getForEntity(getApiUrl() + "/users/" + user.getId() + USER_OU_ENDPOINT_URL, Dhis2OuList.class)
                            .getBody()
                            .getOrganisationUnits()
                            .stream()
                            .map(Dhis2ListEntry::getId)
                            .parallel()
                            .map(id -> getAdminRestTemplate()
                                .getForEntity(getApiUrl() + OU_DETAIL_ENDPOINT_URL, Dhis2OuList.class, id)
                            )
                            .map(ResponseEntity::getBody)
                            .map(Dhis2OuList::getOrganisationUnits)
                            .flatMap(Collection::stream)
                            .distinct(),
                        getAdminRestTemplate()
                            .getForEntity(getApiUrl() + "/users/" + user.getId() + GROUPS_ENDPOINT_URL, Dhis2GroupList.class)
                            .getBody()
                            .getUserGroups()
                            .stream()
                    )
                    .map(Dhis2ListEntry::getDisplayName)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.collectingAndThen(Collectors.toList(), authoritiesMapper::mapAuthorities))
            );

            return usi;
          })
          .collect(Collectors.toList());
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

    userSecurityInfoList.add(anonymous);
    return userSecurityInfoList;
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    Assert.isInstanceOf(
        UsernamePasswordAuthenticationToken.class,
        authentication,
        "Only UsernamePasswordAuthenticationToken is supported"
    );

    final UsernamePasswordAuthenticationToken authToken = (UsernamePasswordAuthenticationToken) authentication;

    String username = authToken.getName();
    String password = (String) authentication.getCredentials();

    RestTemplate restTemplate = new RestTemplate();
    restTemplate.getInterceptors().add(new BasicAuthorizationInterceptor(username, password));

    try {
      restTemplate.headForHeaders(getApiUrl() + ME_ENDPOINT_URL);
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode().equals(HttpStatus.UNAUTHORIZED)) {
        throw new BadCredentialsException("Bad credentials");
      } else {
        throw new AuthenticationServiceException(e.getMessage());
      }
    }

    return new UsernamePasswordAuthenticationToken(
        username,
        password,
        Stream
            .concat(
                restTemplate
                    .getForEntity(getApiUrl() + USER_OU_ENDPOINT_URL, Dhis2OuList.class)
                    .getBody()
                    .getOrganisationUnits()
                    .stream(),
                restTemplate
                    .getForEntity(getApiUrl() + GROUPS_ENDPOINT_URL, Dhis2GroupList.class)
                    .getBody()
                    .getUserGroups()
                    .stream()
            )
            .map(Dhis2ListEntry::getDisplayName)
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.collectingAndThen(Collectors.toList(), authoritiesMapper::mapAuthorities))
    );
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
  }
}
