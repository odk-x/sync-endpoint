package org.opendatakit.common.security.spring.dhis2;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Dhis2User {
  private String id;
  private String name;
  private String displayName;
  private String surname;
  private String firstName;
  private Dhis2UserCredentials userCredentials;
  private List<String> authorities;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getSurname() {
    return surname;
  }

  public void setSurname(String surname) {
    this.surname = surname;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public Dhis2UserCredentials getUserCredentials() {
    return userCredentials;
  }

  public void setUserCredentials(Dhis2UserCredentials userCredentials) {
    this.userCredentials = userCredentials;
  }

  public List<String> getAuthorities() {
    return authorities;
  }

  public void setAuthorities(List<String> authorities) {
    this.authorities = authorities;
  }
}
