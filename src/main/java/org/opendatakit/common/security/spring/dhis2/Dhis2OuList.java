package org.opendatakit.common.security.spring.dhis2;

import java.util.ArrayList;
import java.util.List;

public class Dhis2OuList {
  private List<Dhis2ListEntry> organisationUnits;

  public List<Dhis2ListEntry> getOrganisationUnits() {
    return organisationUnits;
  }

  public void setOrganisationUnits(List<Dhis2ListEntry> organisationUnits) {
    this.organisationUnits = organisationUnits;
  }

  public Dhis2OuList() {
    this.organisationUnits = new ArrayList<>();
  }
}
