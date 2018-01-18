package org.opendatakit.common.security.spring.dhis2;

import java.util.ArrayList;
import java.util.List;

public class Dhis2UserList {
  private List<Dhis2User> users;

  public List<Dhis2User> getUsers() {
    return users;
  }

  public void setUsers(List<Dhis2User> users) {
    this.users = users;
  }

  public Dhis2UserList() {
    this.users = new ArrayList<>();
  }
}
