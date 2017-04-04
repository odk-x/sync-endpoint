package org.opendatakit.common.security.spring;
/*
 * Copyright (C) 2017 University of Washington
 * Copyright 2004, 2005, 2006 Acegi Technology Pty Limited
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

import java.io.IOException;
import java.util.HashSet;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.opendatakit.aggregate.constants.BeanDefs;
import org.opendatakit.aggregate.server.ServerPreferencesProperties;
import org.opendatakit.aggregate.task.Watchdog;
import org.opendatakit.common.persistence.exception.ODKDatastoreException;
import org.opendatakit.common.security.common.GrantedAuthorityName;
import org.opendatakit.common.web.CallingContext;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.Assert;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.filter.GenericFilterBean;

/**
 * Detects if there is no {@code Authentication} object in the
 * {@code SecurityContextHolder}, and populates it with one if needed.
 *
 * @author Ben Alex
 * @author Luke Taylor
 */
public class AnonymousAuthenticationFilter extends GenericFilterBean implements
      InitializingBean {

   // ~ Instance fields
   // ================================================================================================

   private AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource = new WebAuthenticationDetailsSource();
   private String key;
   private Object principal;

   /**
    * Creates a filter with a principal named "anonymousUser" and the single authority
    * "ROLE_ANONYMOUS".
    *
    * @param key the key to identify tokens created by this filter
    */
   public AnonymousAuthenticationFilter(String key) {
      this(key, "anonymousUser");
   }

   /**
    *
    * @param key key the key to identify tokens created by this filter
    * @param principal the principal which will be used to represent anonymous users
    * @param authorities the authority list for anonymous users
    */
   public AnonymousAuthenticationFilter(String key, Object principal) {
      Assert.hasLength(key, "key cannot be null or empty");
      Assert.notNull(principal, "Anonymous authentication principal must be set");
      this.key = key;
      this.principal = principal;
   }

   // ~ Methods
   // ========================================================================================================

   @Override
   public void afterPropertiesSet() {
      Assert.hasLength(key, "key must have length");
      Assert.notNull(principal, "Anonymous authentication principal must be set");
   }

   public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
         throws IOException, ServletException {

      if (SecurityContextHolder.getContext().getAuthentication() == null) {
         SecurityContextHolder.getContext().setAuthentication(
               createAuthentication((HttpServletRequest) req));

         if (logger.isDebugEnabled()) {
            logger.debug("Populated SecurityContextHolder with anonymous token: '"
                  + SecurityContextHolder.getContext().getAuthentication() + "'");
         }
      }
      else {
         if (logger.isDebugEnabled()) {
            logger.debug("SecurityContextHolder not populated with anonymous token, as it already contained: '"
                  + SecurityContextHolder.getContext().getAuthentication() + "'");
         }
      }

      chain.doFilter(req, res);
   }

   public HashSet<GrantedAuthority> getAuthorities(CallingContext cc) {
     boolean anonAccess = false;
     boolean anonDataCollector = false;
     boolean anonTablesSynchronizer = false;
     boolean odkTablesActive = false;
     HashSet<GrantedAuthority> authorities = new HashSet<GrantedAuthority>();
     authorities.add(new SimpleGrantedAuthority(GrantedAuthorityName.USER_IS_ANONYMOUS.name()));

     try {     
       anonAccess = ServerPreferencesProperties.getAnonymousAccessToAttachments(cc);
       anonDataCollector = ServerPreferencesProperties.getAnonymousDataCollection(cc);
       anonTablesSynchronizer = ServerPreferencesProperties.getAnonymousTablesSynchronization(cc);
       odkTablesActive = ServerPreferencesProperties.getOdkTablesEnabled(cc);
     } catch ( ODKDatastoreException e ) {
       e.printStackTrace();
       // user does not have rights to the site.
       return authorities;
     }
     if ( anonAccess ) {
       authorities.add(new SimpleGrantedAuthority(GrantedAuthorityName.ROLE_ATTACHMENT_VIEWER.name()));
     }
     if ( anonDataCollector ) {
       authorities.add(new SimpleGrantedAuthority(GrantedAuthorityName.ROLE_DATA_COLLECTOR.name()));
     }
     if ( odkTablesActive && anonTablesSynchronizer ) {
       authorities.add(new SimpleGrantedAuthority(GrantedAuthorityName.ROLE_USER.name()));
       authorities.add(new SimpleGrantedAuthority(GrantedAuthorityName.ROLE_SYNCHRONIZE_TABLES.name()));
     }
     return authorities;
   }

   protected Authentication createAuthentication(HttpServletRequest request) {
     // we haven't initialized everything yet, but need a daemon context
     // use the one that is created for the watchdog.
     Watchdog watchdog = (Watchdog) WebApplicationContextUtils
         .getRequiredWebApplicationContext(request.getServletContext())
         .getBean(BeanDefs.WATCHDOG);
     
     CallingContext ccDaemon = watchdog.getCallingContext();
     ccDaemon.setAsDaemon(true);

     HashSet<GrantedAuthority> authorities = getAuthorities(ccDaemon);

     AnonymousAuthenticationToken auth = new AnonymousAuthenticationToken(key, principal, authorities);
     auth.setDetails(authenticationDetailsSource.buildDetails(request));

     return auth;
   }

   public void setAuthenticationDetailsSource(
         AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource) {
      Assert.notNull(authenticationDetailsSource,
            "AuthenticationDetailsSource required");
      this.authenticationDetailsSource = authenticationDetailsSource;
   }

   public Object getPrincipal() {
      return principal;
   }
}
