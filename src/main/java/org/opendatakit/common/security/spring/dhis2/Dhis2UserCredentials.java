package org.opendatakit.common.security.spring.dhis2;

public class Dhis2UserCredentials {
  private String id;
  private String username;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }
}
