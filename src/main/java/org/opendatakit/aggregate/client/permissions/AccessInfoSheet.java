/*
 * Copyright (C) 2011 University of Washington
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

package org.opendatakit.aggregate.client.permissions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.opendatakit.aggregate.client.AggregateUI;
import org.opendatakit.aggregate.client.GroupsAndRolesSubTab;
import org.opendatakit.common.security.client.UserSecurityInfo;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class AccessInfoSheet extends Composite {

  private static TemporaryAccessInfoSheetUiBinder uiBinder = GWT
      .create(TemporaryAccessInfoSheetUiBinder.class);

  interface TemporaryAccessInfoSheetUiBinder extends
      UiBinder<Widget, AccessInfoSheet> {
  }

  private final class GrantedAuthorityTextColumn extends TextColumn<String> {

	  GrantedAuthorityTextColumn() {
    	super();
    }

    @Override
    public String getValue(String object) {
    	return object;
    }
  }


  private GroupsAndRolesSubTab groupsAndRolesTab;

  public AccessInfoSheet(GroupsAndRolesSubTab groupsAndRolesTab) {
    this.groupsAndRolesTab = groupsAndRolesTab;
    initWidget(uiBinder.createAndBindUi(this));
    sinkEvents(Event.ONCHANGE | Event.ONCLICK);

    // Username
    GrantedAuthorityTextColumn grants = new GrantedAuthorityTextColumn();
    currentUserTable.addColumn(grants, "Granted Authority");
  }

  @Override
  public void setVisible(boolean isVisible) {
    super.setVisible(isVisible);
    if (isVisible) {
    	redraw(AggregateUI.getUI().getUserInfo());
    }
  }

  private void redraw(UserSecurityInfo userInfo) {
	List<String> names = new ArrayList<String>();
	names.addAll(userInfo.getGrantedAuthorities());
	Collections.sort(names);
	currentUserTable.setRowData(names);
	currentUserTable.redraw();
  }
  
  @UiField
  CellTable<String> currentUserTable;
}
