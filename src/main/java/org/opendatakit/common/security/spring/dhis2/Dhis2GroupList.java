package org.opendatakit.common.security.spring.dhis2;

import java.util.ArrayList;
import java.util.List;

public class Dhis2GroupList {
  private List<Dhis2ListEntry> userGroups;

  public List<Dhis2ListEntry> getUserGroups() {
    return userGroups;
  }

  public void setUserGroups(List<Dhis2ListEntry> userGroups) {
    this.userGroups = userGroups;
  }

  public Dhis2GroupList() {
    this.userGroups = new ArrayList<>();
  }
}
