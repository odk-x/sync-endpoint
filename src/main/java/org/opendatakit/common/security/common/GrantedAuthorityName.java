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
package org.opendatakit.common.security.common;

import java.io.Serializable;


/**
 * Shared code between GWT Javascript and the server side.  This class
 * defines the system-defined granted authority names.  The convention is that:
 * <ul><li>any name beginning with ROLE_ is a primitive authority.</li>
 * <li>any name beginning with RUN_AS_ is a primitive run-as directive.</li>
 * </ul>
 * Only non-primitive names can be granted primitive authorities.
 *
 * @author mitchellsundt@gmail.com
 *
 */
public enum GrantedAuthorityName implements Serializable {

	AUTH_LOCAL("any users authenticated via the locally-held (<em>Aggregate password</em>) credential"),
	AUTH_ACTIVE_DIRECTORY("any users authenticated via Active Directory"),
	AUTH_LDAP("any users authenticated via LDAP"),

	USER_IS_ANONYMOUS("for unauthenticated access"),
	USER_IS_REGISTERED("for registered users of this system (a user identified " +
				"as a registered user will always have been authenticated)"),
	USER_IS_DAEMON("reserved for the execution of background tasks"),

	ROLE_USER("Registered User"),
	ROLE_DATA_COLLECTOR("Data Collector"),
	ROLE_ATTACHMENT_VIEWER("Attachment Viewer"),
	ROLE_DATA_VIEWER("Data Viewer"),
	ROLE_DATA_OWNER("Form Manager"),
   ROLE_SYNCHRONIZE_TABLES("Synchronize Tables"),
   ROLE_SUPER_USER_TABLES("Tables Super-user"),
   ROLE_ADMINISTER_TABLES("Administer Tables"),
	ROLE_SITE_ACCESS_ADMIN("Site Administrator"),
	
	// Synthesized groups from Azure AD groups
   GROUP_DATA_COLLECTORS("Azure AD Group for Data Collector"),
   GROUP_DATA_VIEWERS("Azure AD Group for Data Viewer"),
   GROUP_FORM_MANAGERS("Azure AD Group for Form Manager"),
   GROUP_SYNCHRONIZE_TABLES("Azure AD Group for Synchronize Tables"),
   GROUP_SUPER_USER_TABLES("Azure AD Group for Tables Super-user"),
   GROUP_ADMINISTER_TABLES("Azure AD Group for Administer Tables"),
   GROUP_SITE_ADMINS("Azure AD Group for Site Administrator")
	;

	private String displayText;

	private GrantedAuthorityName() {
	  // GWT
	}

	GrantedAuthorityName(String displayText) {
		this.displayText = displayText;
	}

	public String getDisplayText() {
		return displayText;
	}

   public static final String GROUP_PREFIX = "GROUP_";
	public static final String ROLE_PREFIX = "ROLE_";
	public static final String RUN_AS_PREFIX = "RUN_AS_";

	public static final boolean permissionsCanBeAssigned(String authority) {
		return (authority != null) &&
			!(authority.startsWith(ROLE_PREFIX) || authority.startsWith(RUN_AS_PREFIX));
	}
}
