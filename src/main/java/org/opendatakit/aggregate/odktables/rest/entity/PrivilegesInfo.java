/*
 * Copyright (C) 2017 University of Washington
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

package org.opendatakit.aggregate.odktables.rest.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * This contains the default group of the user and the list of
 * groups and roles to which the currently authenticated user is
 * assigned.
 *
 * @author mitchellsundt@gmail.com
 *
 */
@JacksonXmlRootElement(localName="privilegesInfo")
public class PrivilegesInfo {

  /**
   * Default group
   */
  @JsonProperty(required = false)
  private String defaultGroup;


  /**
   * The roles and groups this user belongs to.
   * This is sorted alphabetically.
   */
  @JsonProperty(required = false)
  @JacksonXmlElementWrapper(useWrapping=false)
  @JacksonXmlProperty(localName="roles")
  private ArrayList<String> roles;

  /**
   * Constructor used by Jackson
   */
  public PrivilegesInfo() {
    this.roles = new ArrayList<String>();
    this.defaultGroup = null;
  }

  /**
   * Constructor used by our Java code
   *
   * @param entries
   */
  public PrivilegesInfo(ArrayList<String> roles,
      String defaultGroup) {
    if (roles == null) {
      this.roles = new ArrayList<String>();
    } else {
      this.roles = roles;
      Collections.sort(this.roles);
    }
    this.defaultGroup = defaultGroup;
  }

  public List<String> getRoles() {
    return roles;
  }

  public void setRoles(ArrayList<String> roles) {
    this.roles = roles;
    Collections.sort(this.roles);
  }

  public String getDefaultGroup() {
    return defaultGroup;
  }

  public void setDefaultGroup(String defaultGroup) {
    this.defaultGroup = defaultGroup;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((roles == null) ? 0 : roles.hashCode());
    result = prime * result + ((defaultGroup == null) ? 0 : defaultGroup.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof PrivilegesInfo)) {
      return false;
    }
    PrivilegesInfo other = (PrivilegesInfo) obj;
    boolean simpleResult = ((roles == null) ? (other.roles == null) :
              ((other.roles != null) && (roles.size() == other.roles.size()))) &&
      (defaultGroup == null ? other.defaultGroup == null : (defaultGroup.equals(other.defaultGroup)));
    
    if ( !simpleResult ) {
      return false;
    }
    
    if ( roles == null ) {
      return true;
    }
    
    // roles is a sorted list. Compare linearly...
    for ( int i = 0 ; i < roles.size() ; ++i ) {
      if ( !roles.get(i).equals(other.roles.get(i)) ) {
        return false;
      }
    }
    return true;
  }
}
